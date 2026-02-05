// components/ui/card.tsx
export const Card = ({ children, ...props }) => (
  <div
    className="rounded-xl border p-4 shadow-md bg-white dark:bg-zinc-900"
    {...props}
  >
    {children}
  </div>
);

export const CardContent = ({ children }) => (
  <div className="mt-2 text-sm text-gray-700 dark:text-gray-300">
    {children}
  </div>
);
