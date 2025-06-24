
"use client";

import { useState, useEffect } from "react";
import { Navbar } from "nextra-theme-docs";
import Image from "next/image";
import { useTheme } from "next-themes";
import YaciStore from "../../utils/icons/YaciStore.svg";
import YaciStoreLight from "../../utils/icons/YaciStoreLight.svg";
import { YACI_STORE_GITHUB_REPO_URL, DISCORD_URL } from "../../utils/constants";

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
        <div style={{ display: "flex", alignItems: "center", height: "100%" }}>
          <Image
            src={logo}
            alt="Veridian Platform Logo"
            height={40}
            style={{ objectFit: "contain" }}
          />
        </div>
      }
      projectLink={YACI_STORE_GITHUB_REPO_URL}
      chatLink={DISCORD_URL}
    />
  );
};

export { NavbarWithThemeLogo };
