window.addEventListener("DOMContentLoaded", (event) => {
    const inputElement = document.querySelector("#file_drop_zone .drop-zone__input");
    const dropZoneElement = document.getElementById("file_drop_zone");
    const formElement = dropZoneElement.closest("form");

    dropZoneElement.addEventListener("click", (e) => {
        inputElement.click();
    });

    inputElement.addEventListener("change", (e) => {
        e.preventDefault();
        if (inputElement.files.length) {
            const fileName = inputElement.files[0].name;
            const userConfirmed = confirm(`Would you like to upload the file "${fileName}"?`);

            if (!userConfirmed) {
                return;
            }
            dropZoneElement.classList.add("drop-zone--over");
            formElement.submit();
        }
    });

    dropZoneElement.addEventListener("dragover", (e) => {
        e.preventDefault();
        dropZoneElement.classList.add("drop-zone--over");
    });


    ["dragleave", "dragend"].forEach((type) => {
        dropZoneElement.addEventListener(type, (e) => {
            dropZoneElement.classList.remove("drop-zone--over");
        });
    });

    dropZoneElement.addEventListener("drop", (e) => {
        e.preventDefault();

        const items = e.dataTransfer.items;

        if (items.length > 0 && items[0].webkitGetAsEntry().isDirectory) {
            const errorMessage = "Folder upload is not supported here";
            if (formElement) {
                const errorInput = document.createElement("input");
                errorInput.type = "hidden";
                errorInput.name = "error";
                errorInput.value = errorMessage;
                formElement.appendChild(errorInput);
                formElement.submit();
            } else {
                console.error("Form not found");
            }

            dropZoneElement.classList.remove("drop-zone--over");
            return;
        }

        if (e.dataTransfer.files.length) {
            const fileName = e.dataTransfer.files[0].name;
            const userConfirmed = confirm(`Would you like to upload the file "${fileName}"?`);
            if (!userConfirmed) {
                return;
            }
            inputElement.files = e.dataTransfer.files;
            dropZoneElement.classList.add("drop-zone--over");
            formElement.submit();
        }
    })
});