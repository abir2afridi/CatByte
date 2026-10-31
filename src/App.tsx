import { useEffect } from 'react'
import { AppLayout } from '@/components/layout/AppLayout'
import { useKeyboardShortcuts } from '@/hooks/useKeyboardShortcuts'
import { useProjectStore } from '@/store/projectStore'

function App() {
  useKeyboardShortcuts()
  const { project, createProject: createProjectAction } = useProjectStore()

  // Create default project on load
  useEffect(() => {
    if (!project) {
      createProjectAction('Untitled Project')
    }
  }, [project, createProjectAction])

  if (!project) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-background">
        <div className="animate-pulse text-muted-foreground">Loading...</div>
      </div>
    )
  }

  return <AppLayout />
}

export default App