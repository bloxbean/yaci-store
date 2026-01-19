<script lang="ts">
    import { page } from '$app/stores';
    import { base } from '$app/paths';

    interface NavItem {
        name: string;
        href: string;
        icon: string;
    }

    const navItems: NavItem[] = [
        { name: 'Dashboard', href: `${base}`, icon: 'home' },
        { name: 'Stores', href: `${base}/stores`, icon: 'database' },
        { name: 'Sync', href: `${base}/sync`, icon: 'refresh' },
        { name: 'Configuration', href: `${base}/config`, icon: 'cog' },
        { name: 'Indexes', href: `${base}/indexes`, icon: 'table' },
        { name: 'Ledger State', href: `${base}/ledger-state`, icon: 'chart' }
    ];

    function isActive(href: string): boolean {
        const currentPath = $page.url.pathname;
        if (href === base || href === `${base}/`) {
            return currentPath === base || currentPath === `${base}/`;
        }
        return currentPath.startsWith(href);
    }

    function getIconPath(icon: string): string {
        const icons: Record<string, string> = {
            home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
            database: 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4',
            refresh: 'M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15',
            cog: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
            table: 'M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z',
            chart: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'
        };
        return icons[icon] || icons.home;
    }
</script>

<aside class="w-64 bg-slate-800 min-h-screen flex flex-col">
    <nav class="flex-1 px-4 py-6 space-y-1">
        {#each navItems as item}
            <a
                href={item.href}
                class="flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-colors {isActive(item.href)
                    ? 'bg-slate-700 text-white'
                    : 'text-slate-300 hover:bg-slate-700 hover:text-white'}"
            >
                <svg class="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={getIconPath(item.icon)} />
                </svg>
                {item.name}
            </a>
        {/each}
    </nav>
    <div class="px-4 py-4 border-t border-slate-700">
        <p class="text-xs text-slate-500">Yaci Store Admin UI</p>
    </div>
</aside>
