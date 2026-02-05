<script lang="ts">
    export let status: 'enabled' | 'disabled' | 'success' | 'error' | 'warning' | 'info';
    export let text: string = '';

    const statusClasses: Record<string, string> = {
        enabled: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
        disabled: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
        success: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
        error: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
        warning: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
        info: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400'
    };

    const defaultTexts: Record<string, string> = {
        enabled: 'Enabled',
        disabled: 'Disabled',
        success: 'Success',
        error: 'Error',
        warning: 'Warning',
        info: 'Info'
    };
</script>

<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium {statusClasses[status]}">
    {text || defaultTexts[status]}
</span>
