window.addEventListener("DOMContentLoaded", (event) => {
    const supportsFileSystemAccessAPI = 'getAsFileSystemHandle' in DataTransferItem.prototype;
    const inputElement = document.querySelector("#folder_drop_zone .drop-zone__input");
    const dropZoneElement = document.getElementById("folder_drop_zone");
    const formElement = dropZoneElement.closest("form");

    if (!inputElement) {
        console.error("Input element for folder upload not found.");
        return;
    }

    dropZoneElement.addEventListener("click", (e) => {
        inputElement.click();
    });

    inputElement.addEventListener("change", async (e) => {
        e.preventDefault();
        if (inputElement.files.length) {
            dropZoneElement.classList.add("drop-zone--over");
            const formData = new FormData(formElement);
            for (let file of inputElement.files) {
                formData.append('uploadedFolder', file, file.name);
            }
            await processDirectoryOnClick(formData);
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

    dropZoneElement.addEventListener('drop', async (e) => {
        e.preventDefault();

        const items = e.dataTransfer.items;

        if (items.length > 0 && items[0].webkitGetAsEntry().isFile) {
            const errorMessage = "File upload is  not supported here";

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

        const userConfirmed = confirm(`Would you like to upload folder "${e.dataTransfer.items[0].webkitGetAsEntry().name}"?`);
        if (!userConfirmed) {
            dropZoneElement.classList.remove("drop-zone--over");
            return;
        }

        dropZoneElement.classList.add("drop-zone--over");
        const formData = new FormData(formElement);

        const fileHandlesPromises = [...e.dataTransfer.items]
            .filter((item) => item.kind === 'file')
            .map((item) =>
                supportsFileSystemAccessAPI
                    ? item.getAsFileSystemHandle()
                    : item.webkitGetAsEntry(),
            );

        const handles = await Promise.all(fileHandlesPromises);

        for (const handle of handles) {
            await processDirectory(handle, formData);
        }

        fetch(formElement.action, {
            method: 'POST',
            body: formData,
        })
            .then(response => {
                if (response.redirected) {
                    window.location.href = response.url;
                } else {
                    return response.text();
                }
            })
            .then(text => {
                console.log('Files uploaded successfully:', text);
            })
            .catch(error => {
                console.error('Error uploading files:', error);
            });
        dropZoneElement.classList.remove("drop-zone--over");
    });

    async function processDirectoryOnClick(formData) {
        if (formData.has('uploadedFolder')) {
            formElement.submit();
        } else {
            console.error("No files to process.");
        }
    }

    async function processDirectory(directoryHandle, formData, currentPath = '') {
        let isEmpty = true;
        const basePath = currentPath ? currentPath + '/' + directoryHandle.name : directoryHandle.name;

        for await (const entry of directoryHandle.values()) {
            const fullPath = basePath + '/' + entry.name + (entry.kind === 'directory' ? '/' : '');

            if (entry.kind === 'file') {
                const file = await entry.getFile();
                formData.append('uploadedFolder', file, fullPath);
                isEmpty = false;
            } else if (entry.kind === 'directory') {
                const isSubfolderEmpty = await processDirectory(entry, formData, basePath);

                if (isSubfolderEmpty) {
                    const emptyDirFile = new Blob([], {type: 'text/plain'});
                    formData.append('uploadedFolder', emptyDirFile, fullPath);
                    console.log(`Empty folder added: ${fullPath}`);
                }
                isEmpty = false;
            }
        }

        if (isEmpty) {
            const emptyDirFile = new Blob([], {type: 'text/plain'});
            const directoryPath = basePath + '/';
            formData.append('uploadedFolder', emptyDirFile, directoryPath);
            console.log(`Empty folder added: ${directoryPath}`);
        }
    }
});
