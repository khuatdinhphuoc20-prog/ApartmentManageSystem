document.querySelectorAll('[data-logout-form]').forEach((form) => {
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                credentials: 'same-origin'
            });

            if (response.ok) {
                window.location.assign(form.dataset.loginUrl);
                return;
            }
        } catch {
            // Let the browser submit the form if the request could not complete.
        }

        form.submit();
    });
});
