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

export function initCamsettingsModule() {
    const netContainer = document.querySelector("#cam-settings-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#cam-settings-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId); // Track timeout
        }
    }

     /** --- 🌐 reboot ფორმა --- */
        const setWRForm = netContainer.querySelector('form[action="/cam-setting-reboot"]');
        const setWRBtn = setWRForm?.querySelector("#cam-settings-reboot-btn");
    
        if (setWRForm && setWRBtn) {
            setWRBtn.replaceWith(setWRBtn.cloneNode(true));
            const newSetWRBtn = netContainer.querySelector("#cam-settings-reboot-btn");
    
            const handler = async (e) => {
                e.preventDefault();
                const formData = new FormData(setWRForm);
                try {
                    const response = await fetch(setWRForm.action, { method: "POST", body: formData });
                    if (!response.ok) throw new Error(`Server error: ${response.status}`);
                    const html = await response.text();
                    content.innerHTML = html;
                    if (content.querySelector("#cam-settings-container")) {
                        const mod = await import(`/scripts/camsettings.js?v=${Date.now()}`);
                        mod.initCamsettingsModule();
                    }
                    updateIndicator(true);
                } catch (err) {
                    content.innerHTML = `<p style="color:red;">Error applying CAMsettings.</p>`;
                    updateIndicator(false);
                    alert("შეცდომა CAM პარამეტრების გამოყენებისას: " + err.message);
                }
            };
            trackEventListener(newSetWRBtn, "click", handler);
        }

    /** --- ⚙️ setComPortSettings ფორმები --- */
    const lanSettingForms = netContainer.querySelectorAll(".cam-settings-from");
    lanSettingForms.forEach(form => {
        const setBtn = form.querySelector(".cam-settings-btn");
        if (setBtn) {
            setBtn.replaceWith(setBtn.cloneNode(true));
            const newSetBtn = form.querySelector(".cam-settings-btn");

            const handler = async (e) => {
                e.preventDefault();
                const formData = new FormData(form);
                try {
                    const response = await fetch(form.action, { method: "POST", body: formData });
                    if (!response.ok) throw new Error(`Server error: ${response.status}`);
                    const result = await response.json();
                    updateIndicator(result.success);
                    if (!result.success && result.message) {
                        alert("CAM პარამეტრების განახლება ჩაიშალა: " + result.message);
                    } else if (!result.success) {
                        alert("CAM პარამეტრების განახლება ჩაიშალა: უცნობი შეცდომა");
                    }
                } catch (err) {
                    content.innerHTML = `<p style="color:red;">Error applying CAM settings.</p>`;
                    updateIndicator(false);
                    alert("შეცდომა CAM პარამეტრების განახლებისას: " + err.message);
                }
            };
            trackEventListener(newSetBtn, "click", handler);
        }
    });
}

export function cleanupCamsettingsModule() {
    // Clear all setTimeouts
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    // Clear all event listeners
    clearEventListeners();

    console.log('✅ Cam module cleaned up');
}