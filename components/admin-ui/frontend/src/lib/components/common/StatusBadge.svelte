<script lang="ts">
    export let status: 'enabled' | 'disabled' | 'success' | 'error' | 'warning' | 'info';
    export let text: string = '';

    const statusClasses: Record<string, string> = {
        enabled: 'bg-green-100 text-green-800',
        disabled: 'bg-gray-100 text-gray-800',
        success: 'bg-green-100 text-green-800',
        error: 'bg-red-100 text-red-800',
        warning: 'bg-yellow-100 text-yellow-800',
        info: 'bg-blue-100 text-blue-800'
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
