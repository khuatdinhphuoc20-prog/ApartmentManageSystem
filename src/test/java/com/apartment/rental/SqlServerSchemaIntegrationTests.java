package com.apartment.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:sqlserver:.*")
class SqlServerSchemaIntegrationTests {

    @Test
    void deployedSchemaHasAllTenTablesAndColumns() throws SQLException {
        Map<String, Integer> expected = Map.of(
            "users", 9,
            "rooms", 11,
            "rental_applications", 11,
            "contracts", 21,
            "contract_co_occupants", 7,
            "billing_cycles", 6,
            "billing_cycle_items", 14,
            "invoices", 23,
            "issues", 12,
            "notifications", 15
        );
        Map<String, Integer> actual = new TreeMap<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT t.name, COUNT(c.column_id) FROM sys.tables t "
                 + "JOIN sys.columns c ON c.object_id = t.object_id "
                 + "WHERE t.schema_id = SCHEMA_ID('dbo') GROUP BY t.name");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                actual.put(rows.getString(1), rows.getInt(2));
            }
        }
        expected.forEach((table, columns) -> assertEquals(columns, actual.get(table), table));
        assertEquals(129, expected.keySet().stream().mapToInt(table -> actual.getOrDefault(table, 0)).sum());
    }

    @Test
    void deployedSchemaHasCriticalDatabaseGuards() throws SQLException {
        try (Connection connection = connect()) {
            assertTrue(scalar(connection,
                "SELECT c.collation_name FROM sys.columns c WHERE c.object_id = OBJECT_ID('dbo.users') AND c.name = 'email'")
                .endsWith("_CS_AS"));

            String occupancyFilter = scalar(connection,
                "SELECT filter_definition FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.contracts') "
                + "AND name = 'ux_contracts_occupying_room' AND is_unique = 1");
            assertTrue(occupancyFilter.contains("SCHEDULED"));
            assertTrue(occupancyFilter.contains("ACTIVE"));

            Map<String, String> invoiceItemForeignKey = new TreeMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT child.name, parent.name FROM sys.foreign_key_columns fkc "
                + "JOIN sys.columns child ON child.object_id = fkc.parent_object_id "
                + "AND child.column_id = fkc.parent_column_id "
                + "JOIN sys.columns parent ON parent.object_id = fkc.referenced_object_id "
                + "AND parent.column_id = fkc.referenced_column_id "
                + "WHERE fkc.constraint_object_id = OBJECT_ID('dbo.fk_invoices_cycle_item_contract')");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    invoiceItemForeignKey.put(rows.getString(1), rows.getString(2));
                }
            }
            assertEquals(Map.of("billing_cycle_item_id", "id", "contract_id", "contract_id"),
                invoiceItemForeignKey);

            for (String checkName : new String[] {
                "ck_rooms_values", "ck_contracts_state", "ck_billing_cycle_items_result",
                "ck_invoices_kind_rule", "ck_invoices_payment", "ck_notifications_reference"
            }) {
                assertEquals("1", scalar(connection,
                    "SELECT CONVERT(varchar(10), COUNT(*)) FROM sys.check_constraints WHERE name = ?", checkName));
            }
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(
            System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
    }

    private static String scalar(Connection connection, String sql, String... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                String value = rows.getString(1);
                assertNotNull(value, sql);
                return value;
            }
        }
    }
}
