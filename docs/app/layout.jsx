import { Head } from "nextra/components";
import "nextra-theme-docs/style.css";
import "../styles/main.css";

export const metadata = {
  title: "Yaci Store - Cardano Blockchain Data Indexing",
  description: "A powerful blockchain data indexing and storage solution for Cardano",
  openGraph: {
    images: "/opengraph-image.jpg",
  },
  twitter: {
    images: "/opengraph-image.jpg",
  },
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <Head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      </Head>
      <body>{children}</body>
    </html>
  );
}
