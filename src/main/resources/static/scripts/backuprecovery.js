let intervalIds = new Set(); // Store setTimeout IDs
let eventListeners = new Map(); // Store event listeners for cleanup

// Helper function to track event listeners
function trackEventListener(element, event, handler) {
    eventListeners.set(element, [...(eventListeners.get(element) || []), { event, handler }]);
    element.addEventListener(event, handler);
}

// Helper function to clear all tracked event listeners
function clearEventListeners() {
    eventListeners.forEach((listeners, element) => {
        listeners.forEach(({ event, handler }) => {
            element.removeEventListener(event, handler);
        });
    });
    eventListeners.clear();
}

// Function to update progress bar
function updateProgressBar(progress) {
    const progressBar = document.querySelector("#backuprecovery-progressBar");
    if (progressBar) {
        progressBar.value = progress;
    }
}

export function initBackuprecoveryModule() {
    const netContainer = document.querySelector("#backuprecovery-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#backuprecovery-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId); // Track timeout
        }
    }

    /** --- 🌐 create backup --- */
    const setWRForm = netContainer.querySelector('form[action="/createbeckup"]');
    const setWRBtn = setWRForm?.querySelector("#backuprecovery-create-btn");

    if (setWRForm && setWRBtn) {
        setWRBtn.replaceWith(setWRBtn.cloneNode(true));
        const newSetWRBtn = netContainer.querySelector("#backuprecovery-create-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(setWRForm);
            try {
                const response = await fetch(setWRForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const html = await response.text();
                content.innerHTML = html;
                if (content.querySelector("#backuprecovery-container")) {
                    const mod = await import(`/scripts/backuprecovery.js?v=${Date.now()}`);
                    mod.initBackuprecoveryModule();
                }
                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error applying BACKUP.</p>`;
                updateIndicator(false);
                alert("შეცდომა ბექაპის გამოყენებისას: " + err.message);
            }
        };
        trackEventListener(newSetWRBtn, "click", handler);
    }

    /** --- 🌐 upload backup --- */
    const uploadForm = netContainer.querySelector('form[action="/uploadbackup"]');
    const uploadBtn = uploadForm?.querySelector('input[type="button"]');
    const fileInput = uploadForm?.querySelector('input[type="file"]');

    if (uploadForm && uploadBtn && fileInput) {
        uploadBtn.replaceWith(uploadBtn.cloneNode(true));
        const newUploadBtn = uploadForm.querySelector('input[type="button"]');

        const handler = async (e) => {
            e.preventDefault();
            const file = fileInput.files[0];
            if (!file) {
                alert("გთხოვთ, აირჩიეთ ფაილი!");
                return;
            }

            // Check file size (100GB = 100 * 1024 * 1024 * 1024 bytes)
            const maxSize = 100 * 1024 * 1024 * 1024;
            if (file.size > maxSize) {
                alert("ფაილი ძალიან დიდია! მაქსიმალური ზომაა 20GB.");
                return;
            }

            const formData = new FormData(uploadForm);
            try {
                // Use XMLHttpRequest for progress tracking
                const xhr = new XMLHttpRequest();
                
                // Progress event listener
                xhr.upload.addEventListener("progress", (event) => {
                    if (event.lengthComputable) {
                        const percentComplete = (event.loaded / event.total) * 100;
                        updateProgressBar(percentComplete);
                    }
                });

                // Completion handler
                xhr.onload = () => {
                    if (xhr.status === 200) {
                        content.innerHTML = xhr.responseText;
                        if (content.querySelector("#backuprecovery-container")) {
                            import(`/scripts/backuprecovery.js?v=${Date.now()}`)
                                .then(mod => mod.initBackuprecoveryModule());
                        }
                        updateIndicator(true);
                        updateProgressBar(0); // Reset progress bar
                    } else {
                        throw new Error(`Server error: ${xhr.status}`);
                    }
                };

                // Error handler
                xhr.onerror = () => {
                    content.innerHTML = `<p style="color:red;">Error uploading backup.</p>`;
                    updateIndicator(false);
                    updateProgressBar(0); // Reset progress bar
                    alert("შეცდომა ფაილის ატვირთვისას.");
                };

                xhr.open("POST", uploadForm.action, true);
                xhr.send(formData);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error uploading backup.</p>`;
                updateIndicator(false);
                updateProgressBar(0); // Reset progress bar
                alert("შეცდომა ფაილის ატვირთვისას: " + err.message);
            }
        };
        trackEventListener(newUploadBtn, "click", handler);
    }
}

export function cleanupBackuprecoveryModule() {
    // Clear all setTimeouts
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    // Clear all event listeners
    clearEventListeners();

    // Reset progress bar
    updateProgressBar(0);

    console.log('✅ BackUp module cleaned up');
}