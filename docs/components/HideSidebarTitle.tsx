"use client";

import { useEffect } from "react";

export default function HideSidebarTitle() {
  useEffect(() => {
    // Function to hide the Index title
    const hideIndexTitle = () => {
      // Check all elements in the sidebar
      const sidebar = document.querySelector('aside.nextra-sidebar-container, aside');

      if (!sidebar) return;

      // Get all elements in the sidebar
      const allElements = sidebar.querySelectorAll('*');

      allElements.forEach(el => {
        const text = el.textContent?.trim();
        const directText = Array.from(el.childNodes)
          .filter(node => node.nodeType === Node.TEXT_NODE)
          .map(node => node.textContent?.trim())
          .join('');

        // Check if this element contains ONLY "Index" text
        if ((text === 'Index' || directText === 'Index') &&
            el.children.length === 0) {
          (el as HTMLElement).style.display = 'none';
          (el as HTMLElement).style.visibility = 'hidden';
          (el as HTMLElement).style.height = '0';
          (el as HTMLElement).style.margin = '0';
          (el as HTMLElement).style.padding = '0';
          (el as HTMLElement).style.overflow = 'hidden';
        }
      });

      // Also try to hide the first child of nav that contains "Index"
      const navs = document.querySelectorAll('aside nav, .nextra-sidebar-container nav');
      navs.forEach(nav => {
        const firstChild = nav.firstElementChild;
        if (firstChild && firstChild.textContent?.trim() === 'Index') {
          (firstChild as HTMLElement).style.display = 'none';
        }
      });
    };

    // Run immediately
    hideIndexTitle();

    // Run after various delays
    setTimeout(hideIndexTitle, 50);
    setTimeout(hideIndexTitle, 100);
    setTimeout(hideIndexTitle, 200);
    setTimeout(hideIndexTitle, 500);
    setTimeout(hideIndexTitle, 1000);

    // Set up a MutationObserver to handle dynamic content
    const observer = new MutationObserver(() => {
      hideIndexTitle();
    });

    const sidebar = document.querySelector('aside.nextra-sidebar-container, aside');

    if (sidebar) {
      observer.observe(sidebar, {
        childList: true,
        subtree: true,
        characterData: true
      });
    }

    return () => observer.disconnect();
  }, []);

  return null;
}
