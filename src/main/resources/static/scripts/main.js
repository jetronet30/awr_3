document.querySelectorAll('.dropdown-toggle').forEach(btn => {
    btn.addEventListener('click', () => {
        const menu = btn.nextElementSibling;
        menu.classList.toggle('open');
    });
});

let contentArea = document.querySelector('.content');
const buttons = document.querySelectorAll('.btn, .drop-btn');
const allButtons = document.querySelectorAll('.btn, .drop-btn, .dropdown-toggle');

// Tracker for loaded scripts and current module
let loadedScripts = new Set();
let currentModule = null;

// Store cleanup functions for each module
const moduleCleanups = new Map();

/**
 * ფუნქცია, რომელიც ასუფთავებს წინა სკრიპტის ეფექტებს
 */
function clearPreviousScripts() {
    // Call cleanup function of the current module, if it exists
    if (currentModule && moduleCleanups.has(currentModule)) {
        try {
            moduleCleanups.get(currentModule)();
            console.log('✅ Previous module cleaned up');
        } catch (err) {
            console.warn('❌ Error during module cleanup:', err);
        }
    }

    // Clear loaded scripts and module data
    loadedScripts.clear();
    moduleCleanups.clear();
    currentModule = null;

    // Remove all event listeners from contentArea (clone and replace method)
    const newContentArea = contentArea.cloneNode(false);
    contentArea.parentNode.replaceChild(newContentArea, contentArea);
    contentArea = newContentArea;
    return contentArea;
}

/**
 * ფუნქცია დინამიურად იტვირთავს JS მოდულს
 * @param {string} src - სკრიპტის ფაილის URL
 */
async function loadModuleScript(src) {
    if (loadedScripts.has(src)) {
        return; // Skip if already loaded
    }
    try {
        const mod = await import(src);
        loadedScripts.add(src);
        const moduleName = src.match(/\/scripts\/(.+)\.js$/)[1];
        const initFunction = `init${moduleName.charAt(0).toUpperCase() + moduleName.slice(1)}Module`;
        const cleanupFunction = `cleanup${moduleName.charAt(0).toUpperCase() + moduleName.slice(1)}Module`;

        // Store cleanup function if it exists
        if (mod[cleanupFunction]) {
            moduleCleanups.set(mod, mod[cleanupFunction]);
        }

        // Initialize the module
        if (mod[initFunction]) {
            mod[initFunction]();
            currentModule = mod;
        }
        console.log(`✅ Module ${src} initialized`);
    } catch (err) {
        console.warn(`❌ Failed to load module: ${src}`, err);
    }
}

/**
 * ფუნქცია, რომელიც HTML-ში ჩასმულ <script type="module"> ტეგებს იტვირთავს
 * @param {HTMLElement} container
 */
function loadInlineScripts(container) {
    const scripts = container.querySelectorAll('script[type="module"]');
    scripts.forEach(s => {
        const src = s.src;
        if (src) loadModuleScript(src).catch(err => console.warn(err));
    });
}

buttons.forEach(button => {
    button.addEventListener('click', async () => {
        allButtons.forEach(b => b.classList.remove('active'));
        button.classList.add('active');
        
        // Clear previous content and scripts
        contentArea.innerHTML = `<p style="color:#00b7eb;">Loading ${button.id}...</p>`;
        clearPreviousScripts(); // Reset contentArea and remove old listeners
        
        const url = button.getAttribute('formaction');

        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `id=${encodeURIComponent(button.id)}`
            });
            if (!response.ok) throw new Error('Server error: ' + response.status);
            const html = await response.text();
            contentArea.innerHTML = html;

            // Load inline scripts from new content
            loadInlineScripts(contentArea);

            // Load corresponding JS module for button.id
            const modulePath = `/scripts/${button.id}.js`;
            try {
                await loadModuleScript(modulePath);
                console.log(`${button.id}.js loaded successfully`);
            } catch (err) {
                console.warn(`Module script not found or failed: ${modulePath}`, err);
            }

        } catch (err) {
            console.error(err);
            contentArea.innerHTML = `<p style="color:red;">შეცდომა მოხდა (${button.id}) მონაცემების ჩატვირთვისას.</p>`;
        }
    });
});

window.addEventListener('beforeunload', () => {
    const activeBtn = document.querySelector('.active');
    if (activeBtn) localStorage.setItem('lastActive', activeBtn.id);
});