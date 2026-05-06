import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { FlexDirection, Overflow, loadYoga } from 'yoga-layout/load'
import type { Config, Node as YogaNodeType } from 'yoga-layout/load'

type YogaTree = {
  root: YogaNodeType
  sidebar: YogaNodeType
  header: YogaNodeType
  main: YogaNodeType
  config: Config
}

type Metrics = { sidebarWidth: number; headerHeight: number }

export function YogaDashboardLayout(props: {
  sidebar: ReactNode
  header: ReactNode
  main: ReactNode
}) {
  const wrapRef = useRef<HTMLDivElement>(null)
  const treeRef = useRef<YogaTree | null>(null)
  const [tree, setTree] = useState<YogaTree | null>(null)
  const [metrics, setMetrics] = useState<Metrics | null>(null)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const Yoga = await loadYoga()
      if (cancelled) return
      const config = Yoga.Config.create()
      config.setUseWebDefaults(true)
      const root = Yoga.Node.create(config)
      root.setFlexDirection(FlexDirection.Row)

      const sidebar = Yoga.Node.create(config)
      sidebar.setWidth(264)
      sidebar.setFlexShrink(0)

      const col = Yoga.Node.create(config)
      col.setFlexGrow(1)
      col.setFlexShrink(1)
      col.setFlexDirection(FlexDirection.Column)

      const header = Yoga.Node.create(config)
      header.setHeight(56)
      header.setFlexShrink(0)

      const main = Yoga.Node.create(config)
      main.setFlexGrow(1)
      main.setFlexShrink(1)
      main.setOverflow(Overflow.Scroll)

      col.insertChild(header, 0)
      col.insertChild(main, 1)
      root.insertChild(sidebar, 0)
      root.insertChild(col, 1)

      const built: YogaTree = { root, sidebar, header, main, config }
      if (cancelled) {
        root.freeRecursive()
        config.free()
        return
      }
      treeRef.current = built
      setTree(built)
    })()
    return () => {
      cancelled = true
      const t = treeRef.current
      treeRef.current = null
      if (t) {
        t.root.freeRecursive()
        t.config.free()
      }
      setTree(null)
      setMetrics(null)
    }
  }, [])

  useLayoutEffect(() => {
    if (!tree) return
    const el = wrapRef.current
    if (!el) return
    const apply = () => {
      const wrap = wrapRef.current
      const tr = treeRef.current
      if (!wrap || !tr) return
      const w = wrap.clientWidth
      const h = wrap.clientHeight
      if (w <= 0 || h <= 0) return
      tr.root.setWidth(w)
      tr.root.setHeight(h)
      tr.root.calculateLayout(w, h)
      setMetrics({
        sidebarWidth: tr.sidebar.getComputedWidth(),
        headerHeight: tr.header.getComputedHeight(),
      })
    }
    apply()
    if (typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [tree])

  const sw = metrics?.sidebarWidth ?? 264
  const hh = metrics?.headerHeight ?? 56

  return (
    <div
      ref={wrapRef}
      className="yoga-root"
      style={{
        display: 'flex',
        flexDirection: 'row',
        width: '100%',
        height: '100vh',
        overflow: 'hidden',
      }}
    >
      <aside
        className="yoga-sidebar"
        style={{
          flex: `0 0 ${sw}px`,
          minWidth: 0,
          overflow: 'auto',
        }}
      >
        {props.sidebar}
      </aside>
      <div
        className="yoga-stage"
        style={{
          flex: 1,
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        <header
          className="yoga-header"
          style={{
            flex: `0 0 ${hh}px`,
            overflow: 'hidden',
          }}
        >
          {props.header}
        </header>
        <main
          className="yoga-main"
          style={{
            flex: 1,
            minHeight: 0,
            overflow: 'auto',
          }}
        >
          {props.main}
        </main>
      </div>
    </div>
  )
}
