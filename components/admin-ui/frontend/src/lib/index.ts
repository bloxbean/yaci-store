// Re-export components for easy importing
export { default as Header } from './components/layout/Header.svelte';
export { default as Sidebar } from './components/layout/Sidebar.svelte';
export { default as PageTitle } from './components/layout/PageTitle.svelte';

export { default as StatusBadge } from './components/common/StatusBadge.svelte';
export { default as Spinner } from './components/common/Spinner.svelte';
export { default as ErrorMessage } from './components/common/ErrorMessage.svelte';

export { default as StatCard } from './components/dashboard/StatCard.svelte';
export { default as SyncProgress } from './components/dashboard/SyncProgress.svelte';
export { default as HealthBadge } from './components/dashboard/HealthBadge.svelte';

export { default as StoreCard } from './components/stores/StoreCard.svelte';
export { default as SyncControls } from './components/sync/SyncControls.svelte';

export * from './api/client';
