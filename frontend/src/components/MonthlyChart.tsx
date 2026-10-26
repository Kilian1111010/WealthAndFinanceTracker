import { useEffect, useRef, useState } from 'react'
import { formatMoney, formatMonth } from '../lib/format'

type MonthValue = {
  month: string
  income: number
  expenses: number
}

type Props = {
  data: MonthValue[]
  currency: string
}

const HEIGHT = 240
const MARGIN = { top: 12, right: 8, bottom: 28, left: 64 }
const TICKS = 4

function niceStep(max: number): number {
  const raw = max / TICKS
  const magnitude = 10 ** Math.floor(Math.log10(raw))
  const normalized = raw / magnitude
  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 2.5 ? 2.5 : normalized <= 5 ? 5 : 10
  return step * magnitude
}

function columnPath(x: number, y: number, width: number, height: number): string {
  const radius = Math.min(4, height, width / 2)
  const bottom = y + height
  return [
    `M${x},${bottom}`,
    `V${y + radius}`,
    `Q${x},${y} ${x + radius},${y}`,
    `H${x + width - radius}`,
    `Q${x + width},${y} ${x + width},${y + radius}`,
    `V${bottom}`,
    'Z',
  ].join(' ')
}

function useWidth<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [width, setWidth] = useState(0)
  useEffect(() => {
    const element = ref.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(element)
    queueMicrotask(() => setWidth(element.getBoundingClientRect().width))
    return () => observer.disconnect()
  }, [])
  return { ref, width }
}

export function MonthlyChart({ data, currency }: Props) {
  const { ref, width } = useWidth<HTMLDivElement>()
  const [active, setActive] = useState<number | null>(null)

  const compact = new Intl.NumberFormat('de-DE', { style: 'currency', currency, notation: 'compact', maximumFractionDigits: 1 })
  const max = Math.max(0, ...data.flatMap((entry) => [entry.income, entry.expenses]))
  const step = max > 0 ? niceStep(max) : 1
  const top = step * TICKS
  const plotWidth = Math.max(0, width - MARGIN.left - MARGIN.right)
  const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom
  const band = data.length > 0 ? plotWidth / data.length : 0
  const barWidth = Math.max(3, Math.min(18, (band - 10) / 2))
  const labelEvery = band < 44 ? 2 : 1
  const y = (value: number) => MARGIN.top + plotHeight - (value / top) * plotHeight

  const activeEntry = active === null ? null : data[active]
  const tooltipOnRight = active !== null && active < data.length / 2
  const tooltipStyle =
    active === null
      ? undefined
      : tooltipOnRight
        ? { left: MARGIN.left + band * (active + 1) + 8, top: MARGIN.top }
        : { left: MARGIN.left + band * active - 8, top: MARGIN.top, transform: 'translateX(-100%)' }

  return (
    <div className="chart">
      <div className="legend" aria-hidden="true">
        <span>
          <i className="swatch income" />
          Einnahmen
        </span>
        <span>
          <i className="swatch expense" />
          Ausgaben
        </span>
      </div>
      <div className="chart-plot" ref={ref} onMouseLeave={() => setActive(null)}>
        {width > 0 && (
          <svg width={width} height={HEIGHT} role="img" aria-label={`Einnahmen und Ausgaben der letzten ${data.length} Monate in ${currency}`}>
            {Array.from({ length: TICKS + 1 }, (_, index) => {
              const value = step * index
              const lineY = y(value)
              return (
                <g key={index}>
                  <line
                    x1={MARGIN.left}
                    x2={width - MARGIN.right}
                    y1={lineY}
                    y2={lineY}
                    className={index === 0 ? 'chart-baseline' : 'chart-grid'}
                  />
                  <text x={MARGIN.left - 8} y={lineY} dy="0.32em" textAnchor="end" className="chart-axis-label">
                    {compact.format(value)}
                  </text>
                </g>
              )
            })}
            {activeEntry && (
              <rect
                x={MARGIN.left + band * active!}
                y={MARGIN.top}
                width={band}
                height={plotHeight}
                className="chart-hover-band"
              />
            )}
            {data.map((entry, index) => {
              const center = MARGIN.left + band * (index + 0.5)
              const incomeHeight = (entry.income / top) * plotHeight
              const expenseHeight = (entry.expenses / top) * plotHeight
              return (
                <g key={entry.month}>
                  {incomeHeight > 0 && (
                    <path d={columnPath(center - barWidth - 1, y(entry.income), barWidth, incomeHeight)} className="bar income" />
                  )}
                  {expenseHeight > 0 && (
                    <path d={columnPath(center + 1, y(entry.expenses), barWidth, expenseHeight)} className="bar expense" />
                  )}
                  {index % labelEvery === (data.length - 1) % labelEvery && (
                    <text x={center} y={HEIGHT - 8} textAnchor="middle" className="chart-axis-label">
                      {formatMonth(entry.month)}
                    </text>
                  )}
                  <rect
                    x={MARGIN.left + band * index}
                    y={MARGIN.top}
                    width={band}
                    height={plotHeight}
                    fill="transparent"
                    onMouseEnter={() => setActive(index)}
                  />
                </g>
              )
            })}
          </svg>
        )}
        {activeEntry && (
          <div className="chart-tooltip" style={tooltipStyle} role="status">
            <strong>{formatMonth(activeEntry.month, 'long')}</strong>
            <span>
              <i className="swatch income" />
              Einnahmen <b>{formatMoney(activeEntry.income, currency)}</b>
            </span>
            <span>
              <i className="swatch expense" />
              Ausgaben <b>{formatMoney(activeEntry.expenses, currency)}</b>
            </span>
            <span className="tooltip-total">
              Saldo <b>{formatMoney(activeEntry.income - activeEntry.expenses, currency)}</b>
            </span>
          </div>
        )}
      </div>
      <details className="chart-table">
        <summary>Als Tabelle anzeigen</summary>
        <table className="table compact">
          <thead>
            <tr>
              <th>Monat</th>
              <th className="number">Einnahmen</th>
              <th className="number">Ausgaben</th>
              <th className="number">Saldo</th>
            </tr>
          </thead>
          <tbody>
            {data.map((entry) => (
              <tr key={entry.month}>
                <td>{formatMonth(entry.month, 'long')}</td>
                <td className="number">{formatMoney(entry.income, currency)}</td>
                <td className="number">{formatMoney(entry.expenses, currency)}</td>
                <td className="number">{formatMoney(entry.income - entry.expenses, currency)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}
