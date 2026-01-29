"use client";

import { useState, useEffect } from "react";
import { Navbar } from "nextra-theme-docs";
import Image from "next/image";
import { useTheme } from "next-themes";
import YaciStore from "../../utils/icons/YaciStoreCube.svg";
import YaciStoreLight from "../../utils/icons/YaciStoreCubeLight.svg";
import { YACI_STORE_GITHUB_REPO_URL, DISCORD_URL } from "../../utils/constants";
import { VersionSelector } from "../VersionSelector/versionSelector";

const NavbarWithThemeLogo = () => {
  const { theme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return null;
  }

  const logo = theme === "dark" ? YaciStore : YaciStoreLight;

  return (
    <Navbar
      logo={
        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <Image
              src={logo}
              alt="Yaci Store Logo"
              height={72}
              style={{
                objectFit: "contain",
                filter: theme === "dark" ? "brightness(1.1) contrast(1.1)" : "none"
              }}
            />
            <span style={{ fontSize: "1.5rem" }}>
              <b>Yaci Store</b>
            </span>
          </div>
          <VersionSelector />
        </div>
      }
      projectLink={YACI_STORE_GITHUB_REPO_URL}
      chatLink={DISCORD_URL}
    />
  );
};

export { NavbarWithThemeLogo };
