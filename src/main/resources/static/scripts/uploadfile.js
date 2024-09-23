window.addEventListener("DOMContentLoaded", (event) => {
    const inputElement = document.querySelector("#file_drop_zone .drop-zone__input");
    const dropZoneElement = document.getElementById("file_drop_zone");
    const formElement = dropZoneElement.closest("form"); // Найти форму

    dropZoneElement.addEventListener("click", (e) => {
        inputElement.click();
    });

    inputElement.addEventListener("change", (e) => {
        if (inputElement.files.length) {
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

        if (e.dataTransfer.files.length) {
            inputElement.files = e.dataTransfer.files;
            formElement.submit(); // Автоматически отправить форму после перетаскивания файла
        }
        dropZoneElement.classList.remove("drop-zone--over");
    })
});