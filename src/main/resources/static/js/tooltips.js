console.log('Tooltips script loaded!');

// Self-executing function to initialize tooltips immediately
(function() {
    console.log('Tooltip initialization starting immediately');

    // Define an array of selectors for tooltip triggering elements
    // This makes it easy to add more selectors in the future
    const tooltipSelectors = [
        '[title]',
        'area[alt]',
        'rect[alt]',
        'polygon[alt]',
        'text[alt]'
    ];

    // Create a combined selector for querying elements
    const combinedSelector = tooltipSelectors.join(', ');
    console.log('Using combined selector for tooltips:', combinedSelector);

    // Create tooltip element
    const tooltip = document.createElement('div');
    tooltip.id = 'custom-tooltip-container';  // Add ID for easy debugging
    tooltip.className = 'custom-tooltip';
    tooltip.style.position = 'absolute';
    tooltip.style.display = 'none';
    tooltip.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
    tooltip.style.color = 'white';
    tooltip.style.padding = '5px 10px';
    tooltip.style.borderRadius = '4px';
    tooltip.style.fontSize = '14px';
    tooltip.style.zIndex = '10000';
    tooltip.style.pointerEvents = 'none';
    tooltip.style.maxWidth = '300px';
    tooltip.style.boxShadow = '0 2px 5px rgba(0, 0, 0, 0.2)';
    document.body.appendChild(tooltip);
    console.log('Tooltip container created with id: custom-tooltip-container');

    // Function to initialize tooltips
    function initTooltips() {
        try {
            // Find elements matching our selectors
            const elementsWithTooltip = document.querySelectorAll(combinedSelector);
//            console.log('Found elements with tooltip attributes:', elementsWithTooltip.length);

            elementsWithTooltip.forEach(element => {
                // Check if element already has a tooltip initialized
                if (element.hasAttribute('data-tooltip-initialized')) {
                    return;
                }

                // Get tooltip text from either title or alt attribute
                let tooltipText = element.getAttribute('title') || element.getAttribute('alt');
                if (!tooltipText) return;

//                console.log('Initializing tooltip for element:', element, 'with text:', tooltipText);

                // Store the tooltip content in a data attribute
                element.setAttribute('data-tooltip', tooltipText);

                // Remove the original attributes to prevent default browser tooltip
                // This prevents the browser's native tooltip from showing in addition to our custom one
                if (element.hasAttribute('title')) element.removeAttribute('title');
                // Only remove alt for non-image elements as it's needed for accessibility on images
                if (element.hasAttribute('alt') && element.tagName !== 'IMG') element.removeAttribute('alt');

                // Mark as initialized
                element.setAttribute('data-tooltip-initialized', 'true');

                // Mouse enter event
                element.addEventListener('mouseenter', (e) => {
                    console.log('Mouse entered element with tooltip:', tooltipText);
                    // Use innerHTML instead of textContent to support HTML formatting
                    tooltip.innerHTML = tooltipText;
                    tooltip.style.display = 'block';
                    updateTooltipPosition(e);
                });

                // Mouse move event
                element.addEventListener('mousemove', updateTooltipPosition);

                // Mouse leave event
                element.addEventListener('mouseleave', () => {
                    tooltip.style.display = 'none';
                });
            });
        } catch (error) {
            console.error('Error initializing tooltips:', error);
        }
    }

    // Update tooltip position based on mouse coordinates
    function updateTooltipPosition(e) {
        const offset = 15; // Distance from cursor

        // Get tooltip dimensions
        const tooltipWidth = tooltip.offsetWidth;
        const tooltipHeight = tooltip.offsetHeight;

        // Calculate position
        let x = e.clientX + offset;
        let y = e.clientY + offset;

        // Check if tooltip would go off screen
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        if (x + tooltipWidth > viewportWidth) {
            x = viewportWidth - tooltipWidth - 5;
        }

        if (y + tooltipHeight > viewportHeight) {
            y = viewportHeight - tooltipHeight - 5;
        }

        // Set the position
        tooltip.style.left = x + 'px';
        tooltip.style.top = y + 'px';
    }

    // If document is already loaded, run initialization now
    if (document.readyState === 'complete' || document.readyState === 'interactive') {
//        console.log('Document already loaded, initializing tooltips now');
        initTooltips();
    } else {
        // Otherwise wait for DOMContentLoaded
//        console.log('Waiting for DOMContentLoaded event');
//        document.addEventListener('DOMContentLoaded', initTooltips);
    }

    // For dynamic content: Re-initialize tooltips when DOM changes
    try {
        // Extract attribute names from selectors for the MutationObserver
        const attributesToObserve = ['title', 'alt'];

        // Using MutationObserver to detect when new elements with tooltip attributes are added
        const observer = new MutationObserver((mutations) => {
            let shouldReinitialize = false;

            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    shouldReinitialize = true;
                } else if (mutation.type === 'attributes' && attributesToObserve.includes(mutation.attributeName)) {
                    shouldReinitialize = true;
                }
            });

            if (shouldReinitialize) {
//                console.log('DOM mutation detected, reinitializing tooltips');
                initTooltips();
            }
        });

        // Start observing the document with the configured parameters
        observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: attributesToObserve
        });
//        console.log('MutationObserver initialized and watching for changes');
    } catch (error) {
        console.error('Error setting up MutationObserver:', error);
    }

})();
