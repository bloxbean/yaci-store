import { writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Theme = 'light' | 'dark' | 'system';
export type ResolvedTheme = 'light' | 'dark';

const STORAGE_KEY = 'yaci-admin-theme';

function getStoredTheme(): Theme {
    if (!browser) return 'dark';
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
        return stored;
    }
    return 'dark';
}

function getSystemTheme(): ResolvedTheme {
    if (!browser) return 'light';
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function resolveTheme(theme: Theme): ResolvedTheme {
    if (theme === 'system') {
        return getSystemTheme();
    }
    return theme;
}

function applyTheme(resolvedTheme: ResolvedTheme) {
    if (!browser) return;
    const root = document.documentElement;
    if (resolvedTheme === 'dark') {
        root.classList.add('dark');
    } else {
        root.classList.remove('dark');
    }
}

const initialTheme = getStoredTheme();
export const theme = writable<Theme>(initialTheme);
export const resolvedTheme = writable<ResolvedTheme>(resolveTheme(initialTheme));

export function setTheme(newTheme: Theme) {
    theme.set(newTheme);
    const resolved = resolveTheme(newTheme);
    resolvedTheme.set(resolved);
    applyTheme(resolved);
    if (browser) {
        localStorage.setItem(STORAGE_KEY, newTheme);
    }
}

export function toggleTheme() {
    let currentResolved: ResolvedTheme = 'light';
    resolvedTheme.subscribe(v => currentResolved = v)();
    const newTheme: Theme = currentResolved === 'dark' ? 'light' : 'dark';
    setTheme(newTheme);
}

export function initializeTheme() {
    if (!browser) return;

    const storedTheme = getStoredTheme();
    const resolved = resolveTheme(storedTheme);
    theme.set(storedTheme);
    resolvedTheme.set(resolved);
    applyTheme(resolved);

    // Listen for system preference changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = () => {
        let currentTheme: Theme = 'system';
        theme.subscribe(v => currentTheme = v)();
        if (currentTheme === 'system') {
            const resolved = getSystemTheme();
            resolvedTheme.set(resolved);
            applyTheme(resolved);
        }
    };
    mediaQuery.addEventListener('change', handleChange);
}
