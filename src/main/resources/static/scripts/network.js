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

export function initNetworkModule() {
    const netContainer = document.querySelector("#network-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#net-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId); // Track timeout
        }
    }

    /** --- 🛰 setNetwork ფორმა --- */
    const setForm = netContainer.querySelector('form[action="/netw-save-and-write"]');
    const setBtn = setForm?.querySelector("#netw-set-and-write-btn");

    if (setForm && setBtn) {
        setBtn.replaceWith(setBtn.cloneNode(true));
        const newSetBtn = netContainer.querySelector("#netw-set-and-write-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(setForm);
            try {
                const response = await fetch(setForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const html = await response.text();
                content.innerHTML = html;
                if (content.querySelector("#network-container")) {
                    const mod = await import(`/scripts/network.js?v=${Date.now()}`);
                    mod.initNetworkModule();
                }
                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error applying network settings.</p>`;
                updateIndicator(false);
                alert("შეცდომა ქსელის პარამეტრების გამოყენებისას: " + err.message);
            }
        };
        trackEventListener(newSetBtn, "click", handler);
    }

    /** --- 🌐 write reboot ფორმა --- */
    const setWRForm = netContainer.querySelector('form[action="/netw-save-and-reboot"]');
    const setWRBtn = setWRForm?.querySelector("#netw-set-and-reboot-btn");

    if (setWRForm && setWRBtn) {
        setWRBtn.replaceWith(setWRBtn.cloneNode(true));
        const newSetWRBtn = netContainer.querySelector("#netw-set-and-reboot-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(setWRForm);
            try {
                const response = await fetch(setWRForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const html = await response.text();
                content.innerHTML = html;
                if (content.querySelector("#network-container")) {
                    const mod = await import(`/scripts/network.js?v=${Date.now()}`);
                    mod.initNetworkModule();
                }
                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error applying network settings.</p>`;
                updateIndicator(false);
                alert("შეცდომა ქსელის პარამეტრების გამოყენებისას: " + err.message);
            }
        };
        trackEventListener(newSetWRBtn, "click", handler);
    }

    /** --- 🌐 testNetwork ფორმა --- */
    const testForm = netContainer.querySelector('form[action="/testNetwork"]');
    const testBtn = testForm?.querySelector("#network-test-btn");

    if (testForm && testBtn) {
        testBtn.replaceWith(testBtn.cloneNode(true));
        const newTestBtn = netContainer.querySelector("#network-test-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(testForm);
            try {
                const response = await fetch(testForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const result = await response.json();
                updateIndicator(result.success);
                if (!result.success) {
                    alert("ქსელის ტესტირება ჩაიშალა");
                }
            } catch (err) {
                updateIndicator(false);
                alert("შეცდომა ქსელის ტესტირებისას: " + err.message);
            }
        };
        trackEventListener(newTestBtn, "click", handler);
    }

    /** --- ⚙️ setLanSettings ფორმები --- */
    const lanSettingForms = netContainer.querySelectorAll(".net-set-from");
    lanSettingForms.forEach(form => {
        const setBtn = form.querySelector(".net-set-btn");
        if (setBtn) {
            setBtn.replaceWith(setBtn.cloneNode(true));
            const newSetBtn = form.querySelector(".net-set-btn");

            const handler = async (e) => {
                e.preventDefault();
                const formData = new FormData(form);
                try {
                    const response = await fetch(form.action, { method: "POST", body: formData });
                    if (!response.ok) throw new Error(`Server error: ${response.status}`);
                    const result = await response.json();
                    updateIndicator(result.success);
                    if (!result.success && result.message) {
                        alert("LAN პარამეტრების განახლება ჩაიშალა: " + result.message);
                    } else if (!result.success) {
                        alert("LAN პარამეტრების განახლება ჩაიშალა: უცნობი შეცდომა");
                    }
                } catch (err) {
                    content.innerHTML = `<p style="color:red;">Error applying LAN settings.</p>`;
                    updateIndicator(false);
                    alert("შეცდომა LAN პარამეტრების განახლებისას: " + err.message);
                }
            };
            trackEventListener(newSetBtn, "click", handler);
        }
    });
}

export function cleanupNetworkModule() {
    // Clear all setTimeouts
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    // Clear all event listeners
    clearEventListeners();

    console.log('✅ Network module cleaned up');
}