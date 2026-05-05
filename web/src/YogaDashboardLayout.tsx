import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { FlexDirection, Overflow, loadYoga } from 'yoga-layout/load'
import type { Config, Node as YogaNodeType } from 'yoga-layout/load'

type Rect = { left: number; top: number; width: number; height: number }

function toRect(n: YogaNodeType): Rect {
  return {
    left: n.getComputedLeft(),
    top: n.getComputedTop(),
    width: n.getComputedWidth(),
    height: n.getComputedHeight(),
  }
}

type YogaTree = {
  root: YogaNodeType
  sidebar: YogaNodeType
  header: YogaNodeType
  main: YogaNodeType
  config: Config
}

export function YogaDashboardLayout(props: {
  sidebar: ReactNode
  header: ReactNode
  main: ReactNode
}) {
  const wrapRef = useRef<HTMLDivElement>(null)
  const treeRef = useRef<YogaTree | null>(null)
  const [tree, setTree] = useState<YogaTree | null>(null)
  const [rects, setRects] = useState<{
    sidebar: Rect
    header: Rect
    main: Rect
  } | null>(null)

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
      sidebar.setWidth(280)
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
      setRects(null)
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
      setRects({
        sidebar: toRect(tr.sidebar),
        header: toRect(tr.header),
        main: toRect(tr.main),
      })
    }
    apply()
    if (typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [tree])

  return (
    <div ref={wrapRef} className="yoga-root">
      {rects && (
        <>
          <div
            className="yoga-pane yoga-sidebar"
            style={{
              position: 'absolute',
              left: rects.sidebar.left,
              top: rects.sidebar.top,
              width: rects.sidebar.width,
              height: rects.sidebar.height,
              boxSizing: 'border-box',
              overflow: 'auto',
            }}
          >
            {props.sidebar}
          </div>
          <div
            className="yoga-pane yoga-header"
            style={{
              position: 'absolute',
              left: rects.header.left,
              top: rects.header.top,
              width: rects.header.width,
              height: rects.header.height,
              boxSizing: 'border-box',
              overflow: 'hidden',
            }}
          >
            {props.header}
          </div>
          <div
            className="yoga-pane yoga-main"
            style={{
              position: 'absolute',
              left: rects.main.left,
              top: rects.main.top,
              width: rects.main.width,
              height: rects.main.height,
              boxSizing: 'border-box',
              overflow: 'auto',
            }}
          >
            {props.main}
          </div>
        </>
      )}
    </div>
  )
}
