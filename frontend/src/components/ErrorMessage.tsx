export function ErrorMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="p-6">
      <p className="text-red-600">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          Tentar novamente
        </button>
      )}
    </div>
  )
}
