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
                    credentials: 'include',
                    body: new URLSearchParams({
                        'newName': text,
                        'oldName': oldName
                    })
                })
                    .then(response => response.text())
                    .then(data => {
                        if (data.startsWith('redirect:')) {
                            window.location.href = data.substring('redirect:'.length);
                        } else {
                            console.log(data);
                        }
                    })
                    .catch(error => console.error('Error:', error));

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