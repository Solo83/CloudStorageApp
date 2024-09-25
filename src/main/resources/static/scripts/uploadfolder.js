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
        if (inputElement.files.length) {
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
                console.log('Файлы успешно загружены:', text);
            })
            .catch(error => {
                console.error('Ошибка загрузки файлов:', error);
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
        let isEmpty = true; // Флаг для определения, есть ли файлы в каталоге

        // Используйте текущий путь для накопления полного пути
        const basePath = currentPath ? currentPath + '/' + directoryHandle.name : directoryHandle.name;

        // Обходим все записи в директории
        for await (const entry of directoryHandle.values()) {
            const fullPath = basePath + '/' + entry.name + (entry.kind === 'directory' ? '/' : '');

            if (entry.kind === 'file') {
                // Если это файл, добавляем его в formData
                const file = await entry.getFile();
                formData.append('uploadedFolder', file, fullPath);
                isEmpty = false; // Найден файл, директория не пустая
            } else if (entry.kind === 'directory') {
                // Если это директория, рекурсивно обрабатываем её
                const isSubfolderEmpty = await processDirectory(entry, formData, basePath); // Получаем статус подпапки

                // Если подпапка оказалась пустой, добавляем пустой файл для неё
                if (isSubfolderEmpty) {
                    const emptyDirFile = new Blob([], {type: 'text/plain'}); // Создаём пустой файл
                    formData.append('uploadedFolder', emptyDirFile, fullPath); // Добавляем пустую директорию в formData
                    console.log(`Пустая папка добавлена: ${fullPath}`);
                }
                isEmpty = false; // Найдена хотя бы одна подпапка, не пустая директория
            }
        }

        // Если директория пустая, добавляем пустой файл
        if (isEmpty) {
            const emptyDirFile = new Blob([], {type: 'text/plain'}); // Создаём пустой файл
            const directoryPath = basePath + '/'; // Полный путь для пустой директории
            formData.append('uploadedFolder', emptyDirFile, directoryPath); // Добавляем пустую директорию в formData
            console.log(`Пустая папка добавлена: ${directoryPath}`);
        }
    }
});
