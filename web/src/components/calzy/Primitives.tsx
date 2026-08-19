import type { LucideIcon } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useRef, useState, type ReactNode } from "react";

import { cn } from "@/lib/utils";

/** Frosted card container — CardBackground in Theme.swift. */
export function Card({
  children,
  className,
  radius = 24,
  padding = 18,
}: {
  children: ReactNode;
  className?: string;
  radius?: number;
  padding?: number;
}): JSX.Element {
  return (
    <div
      className={cn("calzy-card", className)}
      style={{ borderRadius: radius, padding }}
    >
      {children}
    </div>
  );
}

/** Number that rolls up when its value changes. */
export function AnimatedNumber({
  value,
  className,
  duration = 520,
}: {
  value: number;
  className?: string;
  duration?: number;
}): JSX.Element {
  const [display, setDisplay] = useState<number>(value);
  const fromRef = useRef<number>(value);
  const frameRef = useRef<number | null>(null);

  useEffect(() => {
    const from = fromRef.current;
    if (from === value) return;
    const start = performance.now();

    const tick = (now: number): void => {
      const t = Math.min(1, (now - start) / duration);
      // easeOutExpo — feels like SwiftUI's numericText spring.
      const eased = t === 1 ? 1 : 1 - Math.pow(2, -10 * t);
      setDisplay(Math.round(from + (value - from) * eased));
      if (t < 1) {
        frameRef.current = requestAnimationFrame(tick);
      } else {
        fromRef.current = value;
      }
    };

    frameRef.current = requestAnimationFrame(tick);
    return () => {
      if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
      fromRef.current = value;
    };
  }, [value, duration]);

  return <span className={cn("metric tabular-nums", className)}>{display}</span>;
}

/** Animated circular progress ring used for calories, water and macros. */
export function RingProgress({
  progress,
  size = 118,
  lineWidth = 11,
  gradient,
  trackColor = "rgba(0,0,0,0.06)",
  children,
}: {
  progress: number;
  size?: number;
  lineWidth?: number;
  gradient: [string, string];
  trackColor?: string;
  children?: ReactNode;
}): JSX.Element {
  const [animated, setAnimated] = useState<number>(0);
  const gradientId = useRef<string>(`ring-${Math.random().toString(36).slice(2, 9)}`);

  useEffect(() => {
    const timer = window.setTimeout(() => setAnimated(progress), 60);
    return () => window.clearTimeout(timer);
  }, [progress]);

  const radius = (size - lineWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.min(Math.max(animated, 0), 1);

  const overRadius = radius - lineWidth * 0.9;
  const overCircumference = 2 * Math.PI * Math.max(overRadius, 1);
  const overflow = Math.min(Math.max(animated - 1, 0), 1);

  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <defs>
          <linearGradient id={gradientId.current} x1="0%" y1="0%" x2="70%" y2="100%">
            <stop offset="0%" stopColor={gradient[0]} />
            <stop offset="100%" stopColor={gradient[1]} />
          </linearGradient>
          <linearGradient id={`${gradientId.current}-over`} x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="hsl(var(--flame))" />
            <stop offset="100%" stopColor="hsl(var(--protein))" />
          </linearGradient>
        </defs>
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={trackColor}
          strokeWidth={lineWidth}
          strokeLinecap="round"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={`url(#${gradientId.current})`}
          strokeWidth={lineWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - clamped)}
          style={{ transition: "stroke-dashoffset 900ms cubic-bezier(0.22, 1, 0.36, 1)" }}
        />
        {overflow > 0 && overRadius > 2 && (
          <circle
            cx={size / 2}
            cy={size / 2}
            r={overRadius}
            fill="none"
            stroke={`url(#${gradientId.current}-over)`}
            strokeWidth={lineWidth * 0.45}
            strokeLinecap="round"
            strokeDasharray={overCircumference}
            strokeDashoffset={overCircumference * (1 - overflow)}
            style={{ transition: "stroke-dashoffset 900ms cubic-bezier(0.22, 1, 0.36, 1)" }}
          />
        )}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">{children}</div>
    </div>
  );
}

/** A small linear bar with animated fill, used in macro breakdowns. */
export function MacroBar({
  progress,
  color,
  height = 6,
}: {
  progress: number;
  color: string;
  height?: number;
}): JSX.Element {
  const [animated, setAnimated] = useState<number>(0);

  useEffect(() => {
    const timer = window.setTimeout(() => setAnimated(progress), 40);
    return () => window.clearTimeout(timer);
  }, [progress]);

  return (
    <div className="w-full overflow-hidden rounded-full bg-black/[0.06]" style={{ height }}>
      <div
        className="h-full rounded-full"
        style={{
          width: `${Math.min(Math.max(animated, 0), 1) * 100}%`,
          background: `linear-gradient(to right, ${color}bf, ${color})`,
          transition: "width 800ms cubic-bezier(0.22, 1, 0.36, 1)",
        }}
      />
    </div>
  );
}

/** Section header with an icon and optional trailing slot. */
export function SectionHeader({
  icon: Icon,
  title,
  trailing,
  className,
}: {
  icon: LucideIcon;
  title: string;
  trailing?: ReactNode;
  className?: string;
}): JSX.Element {
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <Icon size={16} className="text-ink-soft" strokeWidth={2.4} />
      <h2 className="text-[19px] font-bold text-ink">{title}</h2>
      <div className="ml-auto">{trailing}</div>
    </div>
  );
}

/** Full-width dark pill — the primary action across the app. */
export function PrimaryButton({
  children,
  onClick,
  disabled,
  className,
  type = "button",
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
  type?: "button" | "submit";
}): JSX.Element {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "pressable flex w-full items-center justify-center gap-2 rounded-full bg-ink px-5 py-[17px] text-[17px] font-semibold text-white shadow-[0_10px_24px_rgba(0,0,0,0.18)] disabled:opacity-40",
        className,
      )}
    >
      {children}
    </button>
  );
}

/** iOS-style segmented control. */
export function Segmented<T extends string>({
  options,
  value,
  onChange,
  className,
}: {
  options: ReadonlyArray<{ value: NoInfer<T>; label: string }>;
  value: T;
  onChange: (value: T) => void;
  className?: string;
}): JSX.Element {
  return (
    <div className={cn("flex rounded-[10px] bg-black/[0.055] p-[2px]", className)}>
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            onClick={() => onChange(option.value)}
            className={cn(
              "flex-1 rounded-lg px-3 py-[6px] text-[13px] font-semibold transition-all duration-200",
              active ? "bg-white text-ink shadow-[0_1px_4px_rgba(0,0,0,0.12)]" : "text-ink-soft",
            )}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}

/** iOS-style toggle switch. */
export function Toggle({
  checked,
  onChange,
  tint = "hsl(var(--ink))",
}: {
  checked: boolean;
  onChange: (value: boolean) => void;
  tint?: string;
}): JSX.Element {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="relative h-[31px] w-[51px] shrink-0 rounded-full transition-colors duration-300"
      style={{ backgroundColor: checked ? tint : "rgba(0,0,0,0.1)" }}
    >
      <span
        className="absolute top-[2px] block h-[27px] w-[27px] rounded-full bg-white shadow-[0_2px_5px_rgba(0,0,0,0.22)] transition-transform duration-300"
        style={{ transform: `translateX(${checked ? 22 : 2}px)` }}
      />
    </button>
  );
}

/** Range input wired to the shared Calzy slider styling. */
export function Slider({
  value,
  min,
  max,
  step,
  onChange,
  tint = "hsl(var(--ink))",
  disabled,
}: {
  value: number;
  min: number;
  max: number;
  step: number;
  onChange: (value: number) => void;
  tint?: string;
  disabled?: boolean;
}): JSX.Element {
  const fill = max > min ? ((value - min) / (max - min)) * 100 : 0;
  return (
    <input
      type="range"
      className="calzy-slider disabled:opacity-50"
      value={value}
      min={min}
      max={max}
      step={step}
      disabled={disabled}
      onChange={(event) => onChange(Number(event.target.value))}
      style={
        {
          "--slider-tint": tint,
          "--slider-fill": `${fill}%`,
        } as React.CSSProperties
      }
    />
  );
}
