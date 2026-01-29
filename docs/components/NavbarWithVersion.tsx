"use client";

import { Navbar } from "nextra-theme-docs";
import { ModernVersionSelector } from "./ModernVersionSelector";

const NavbarWithVersion = () => {
  return (
    <div style={{ position: 'relative' }}>
      <Navbar
        logo={
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <img
                src="/images/YaciStore.svg"
                alt="Yaci Store Logo"
                style={{ height: '72px' }}
              />
              <span style={{ fontSize: '1.5rem' }}><b>Yaci Store</b></span>
            </div>
            <ModernVersionSelector />
          </div>
        }
        projectLink="https://github.com/bloxbean/yaci-store"
        chatLink="https://discord.gg/JtQ54MSw6p"
      />
    </div>
  );
};

export { NavbarWithVersion };
