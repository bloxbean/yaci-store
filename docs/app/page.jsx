import Landing from "../components/landing/Landing";

export const metadata = {
  title: { absolute: "Yaci Store · Modular Cardano indexer" },
  description:
    "Yaci Store is a modular, open-source Java indexer for Cardano. Run it with Docker or embed the stores you need as Spring Boot starters, and serve the data over REST, Blockfrost-compatible APIs, SQL, Parquet or MCP.",
};

export default function Page() {
  return <Landing />;
}
