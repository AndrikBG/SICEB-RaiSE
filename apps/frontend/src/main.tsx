import { StrictMode, Component, type ErrorInfo, type ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App'

class RootErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  constructor(props: { children: ReactNode }) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error: Error) {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Error renderizando la aplicación:', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-6 text-center">
          <h1 className="text-lg font-semibold text-gray-900 mb-2">No se pudo cargar la aplicación</h1>
          <p className="text-sm text-gray-600 max-w-md mb-4">
            {this.state.error.message}
          </p>
          <button
            type="button"
            className="text-sm text-purple-600 underline"
            onClick={() => globalThis.location.reload()}
          >
            Recargar página
          </button>
        </div>
      )
    }
    return this.props.children
  }
}

const rootEl = document.getElementById('root')
if (!rootEl) {
  throw new Error('Elemento #root no encontrado en index.html')
}

createRoot(rootEl).render(
  <StrictMode>
    <RootErrorBoundary>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </RootErrorBoundary>
  </StrictMode>,
)
