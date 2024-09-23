function confirmRename(button) {
    const confirmMessage = button.getAttribute('data-confirm-rename');
    return confirm(confirmMessage);
}

function focusOnRenamedObject(button) {
    if(confirmRename(button)){
        const focusedId = button.getAttribute('data-target');
        const oldName = button.getAttribute('data-oldURL');
        const element = document.getElementById(focusedId);
        const originalText = element.innerText;

        element.contentEditable="true";
        element.focus();
        element.addEventListener("keydown", function(event) {
            if (event.key === "Enter") {
                event.preventDefault();
                const text = element.innerText;
                console.log("Введённый текст:", text);

                fetch('home/rename', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: new URLSearchParams({
                        'newName': text,
                        'oldName': oldName
                    })
                }).then(r => window.location.reload());

                element.contentEditable = "false";
            }

            if (event.key === "Escape") {
                element.innerText = originalText;
                element.contentEditable = "false";
                event.preventDefault();
            }
        });
    }
}