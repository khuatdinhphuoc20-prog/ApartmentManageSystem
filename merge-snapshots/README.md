# Conflict resolution record

This merge keeps the current `billing-module` versions of the three canonical documents because they match the active per-contract `BillingPeriod` implementation. The versions from `origin/main` are preserved byte-for-byte under `merge-snapshots/main/` for review; they describe a different billing policy based on a fixed day-10 cycle. No side was silently rewritten to imitate the other.

The source merge keeps the local billing implementation and adds the account, application, and room code from `origin/main`. The two repository APIs are combined. The duplicate legacy login/security configuration was consolidated into the account security configuration, including the local payment callback and payment-role rules.
