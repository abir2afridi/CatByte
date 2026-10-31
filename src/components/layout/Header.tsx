'use client'

import { useState } from 'react'
import { X, Save, Undo2, Redo2, Maximize2, Minimize2, FileText, FolderOpen, Download, Settings, ChevronDown, Monitor, Smartphone, Tablet, Zap, Bot, Share2, HelpCircle } from 'lucide-react'
import { useProjectStore } from '@/store/projectStore'
import { exportPresets } from '@/types'

interface HeaderProps {
  projectName: string
}

export function Header({ projectName }: HeaderProps) {
  const [showExportMenu, setShowExportMenu] = useState(false)
  const [showProjectMenu, setShowProjectMenu] = useState(false)
  const [showViewMenu, setShowViewMenu] = useState(false)
  const { project, saveProject, undo, redo, canUndo, canRedo, exportSettings, updateExportSettings } = useProjectStore()

  const handleNewProject = () => {
    // TODO: Check for unsaved changes
    useProjectStore.getState().createProject('Untitled Project')
  }

  const handleSave = async () => {
    await saveProject()
  }

  const handleExport = async (preset?: typeof exportPresets[0]) => {
    if (preset) {
      updateExportSettings({
        width: preset.width,
        height: preset.height,
        frameRate: preset.frameRate,
        bitrate: preset.bitrate,
        audioBitrate: preset.audioBitrate,
      })
    }
    setShowExportMenu(false)
    // TODO: Open export dialog
    console.log('Export with settings:', exportSettings)
  }

  const handleUndo = () => {
    if (canUndo) undo()
  }

  const handleRedo = () => {
    if (canRedo) redo()
  }

  return (
    <header className="flex h-10 items-center gap-2 border-b border-border bg-secondary/50 px-3 backdrop-blur-sm z-30">
      {/* App Menu Button */}
      <button
        className="btn-ghost h-8 w-8 p-0"
        onClick={() => setShowProjectMenu(!showProjectMenu)}
        aria-label="Project menu"
      >
        <FileText className="h-4 w-4" />
      </button>

      {/* Project Menu Dropdown */}
      {showProjectMenu && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setShowProjectMenu(false)} />
          <div className="fixed top-10 left-3 z-50 panel min-w-[180px] animate-slide-down">
            <div className="p-1">
              <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={handleNewProject}>
                <FileText className="h-4 w-4" /> New Project
              </button>
              <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => {}}>
                <FolderOpen className="h-4 w-4" /> Open Project
              </button>
              <div className="separator-horizontal my-1" />
              <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={handleSave} disabled={!project?.updatedAt}>
                <Save className="h-4 w-4" /> Save
              </button>
              <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => {}}>
                <Download className="h-4 w-4" /> Save As...
              </button>
              <div className="separator-horizontal my-1" />
              <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => setShowExportMenu(true)}>
                <Share2 className="h-4 w-4" /> Export
              </button>
              <div className="separator-horizontal my-1" />
              <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => {}}>
                <Settings className="h-4 w-4" /> Preferences
              </button>
            </div>
          </div>
        </>
      )}

      {/* Project Name */}
      <div className="flex-1 flex items-center gap-2 truncate">
        <span className="font-medium truncate">{projectName}</span>
        {project && (
          <span className="text-xs text-muted-foreground px-2 py-0.5 rounded bg-muted">
            {project.settings.width}×{project.settings.height} @ {project.settings.frameRate}fps
          </span>
        )}
      </div>

      {/* Center - Transport Controls */}
      <div className="flex items-center gap-1 px-4">
        <button className="btn-ghost h-8 w-8 p-0" onClick={handleUndo} disabled={!canUndo} aria-label="Undo">
          <Undo2 className="h-4 w-4" />
        </button>
        <button className="btn-ghost h-8 w-8 p-0" onClick={handleRedo} disabled={!canRedo} aria-label="Redo">
          <Redo2 className="h-4 w-4" />
        </button>
        <div className="separator-vertical h-5 mx-1" />
      </div>

      {/* Right - Export & View */}
      <div className="flex items-center gap-1 ml-auto">
        {/* Export Button */}
        <div className="relative">
          <button
            className="btn-primary h-8 px-3 gap-2"
            onClick={() => setShowExportMenu(!showExportMenu)}
            aria-label="Export"
          >
            <Share2 className="h-4 w-4" />
            <span className="hidden sm:inline">Export</span>
          </button>

          {showExportMenu && (
            <>
              <div className="fixed inset-0 z-40" onClick={() => setShowExportMenu(false)} />
              <div className="fixed top-10 right-3 z-50 panel min-w-[220px] animate-slide-down">
                <div className="p-2">
                  <p className="px-2 py-1 text-xs font-medium text-muted-foreground">Quick Export</p>
                  {exportPresets.map((preset) => (
                    <button
                      key={preset.name}
                      className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent"
                      onClick={() => handleExport(preset)}
                    >
                      <Monitor className="h-4 w-4" />
                      <span>{preset.name}</span>
                      <span className="ml-auto text-xs text-muted-foreground">
                        {preset.width}×{preset.height}
                      </span>
                    </button>
                  ))}
                  <div className="separator-horizontal my-1" />
                  <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => {}}>
                    <Settings className="h-4 w-4" /> Custom Export...
                  </button>
                </div>
              </div>
            </>
          )}
        </div>

        {/* View Menu */}
        <div className="relative">
          <button
            className="btn-ghost h-8 w-8 p-0"
            onClick={() => setShowViewMenu(!showViewMenu)}
            aria-label="View options"
          >
            <Maximize2 className="h-4 w-4" />
          </button>
          {showViewMenu && (
            <>
              <div className="fixed inset-0 z-40" onClick={() => setShowViewMenu(false)} />
              <div className="fixed top-10 right-48 z-50 panel min-w-[160px] animate-slide-down">
                <div className="p-1">
                  <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent">
                    <Monitor className="h-4 w-4" /> Toggle Fullscreen
                  </button>
                  <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent">
                    <Smartphone className="h-4 w-4" /> Mobile Preview
                  </button>
                  <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent">
                    <Tablet className="h-4 w-4" /> Tablet Preview
                  </button>
                  <div className="separator-horizontal my-1" />
                  <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent">
                    <Zap className="h-4 w-4" /> AI Features
                  </button>
                  <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent">
                    <Bot className="h-4 w-4" /> Auto Captions
                  </button>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Help */}
        <button className="btn-ghost h-8 w-8 p-0" aria-label="Help">
          <HelpCircle className="h-4 w-4" />
        </button>
      </div>
    </header>
  )
}