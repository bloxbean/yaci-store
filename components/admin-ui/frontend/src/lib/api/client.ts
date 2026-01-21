const API_BASE = '/api/admin-ui';

export interface SyncStatus {
    block: number;
    slot: number;
    epoch: number;
    era: string;
    blockHash: string;
    syncPercentage: number;
    networkBlock: number;
    networkSlot: number;
    synced: boolean;
}

export interface HealthStatus {
    connectionAlive: boolean;
    receivingBlocks: boolean;
    scheduledToStop: boolean;
    error: boolean;
    lastReceivedBlockTime: number;
    timeSinceLastBlock: number;
    blockReceiveDelayThreshold: number;
}

export interface StoreStatus {
    name: string;
    enabled: boolean;
    apiEnabled: boolean;
}

export interface ConfigSection {
    name: string;
    properties: Record<string, string | number | boolean>;
}

export interface IndexStatus {
    name: string;
    tableName: string;
    exists: boolean;
    columns: string[];
}

export interface LedgerStateStatus {
    enabled: boolean;
    currentEpoch: number;
    lastProcessedEpoch: number;
    jobRunning: boolean;
    lastJobStatus: string;
    lastJobError: string | null;
    lastErrorEpoch: number | null;
    lastJobTimestamp: number | null;
}

export interface UiSettings {
    headerText: string;
}

async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
    }
    return response.json();
}

export const api = {
    getStores: (): Promise<StoreStatus[]> =>
        fetch(`${API_BASE}/stores`).then(r => handleResponse<StoreStatus[]>(r)),

    getConfig: (): Promise<ConfigSection[]> =>
        fetch(`${API_BASE}/config`).then(r => handleResponse<ConfigSection[]>(r)),

    getUiSettings: (): Promise<UiSettings> =>
        fetch(`${API_BASE}/settings`).then(r => handleResponse<UiSettings>(r)),

    getSyncStatus: (): Promise<SyncStatus> =>
        fetch(`${API_BASE}/sync/status`).then(r => handleResponse<SyncStatus>(r)),

    getSyncControlEnabled: (): Promise<boolean> =>
        fetch(`${API_BASE}/sync/control-enabled`)
            .then(r => handleResponse<{ enabled: boolean }>(r))
            .then(data => data.enabled),

    getHealth: (): Promise<HealthStatus> =>
        fetch(`${API_BASE}/health`).then(r => handleResponse<HealthStatus>(r)),

    getIndexes: (): Promise<IndexStatus[]> =>
        fetch(`${API_BASE}/indexes`).then(r => handleResponse<IndexStatus[]>(r)),

    refreshIndexes: (): Promise<IndexStatus[]> =>
        fetch(`${API_BASE}/indexes/refresh`, { method: 'POST' }).then(r => handleResponse<IndexStatus[]>(r)),

    getLedgerState: (): Promise<LedgerStateStatus> =>
        fetch(`${API_BASE}/ledger-state`).then(r => handleResponse<LedgerStateStatus>(r)),

    startSync: (): Promise<void> =>
        fetch(`${API_BASE}/sync/start`, { method: 'POST' }).then(r => {
            if (!r.ok) throw new Error('Failed to start sync');
        }),

    stopSync: (): Promise<void> =>
        fetch(`${API_BASE}/sync/stop`, { method: 'POST' }).then(r => {
            if (!r.ok) throw new Error('Failed to stop sync');
        }),

    restartSync: (): Promise<void> =>
        fetch(`${API_BASE}/sync/restart`, { method: 'POST' }).then(r => {
            if (!r.ok) throw new Error('Failed to restart sync');
        })
};
