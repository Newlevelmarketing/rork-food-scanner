import type { JSX } from "react";

import {
  Armchair,
  Bike,
  Dumbbell,
  Flame,
  Flower2,
  Footprints,
  Goal,
  Mountain,
  PersonStanding,
  Sailboat,
  Swords,
  Trophy,
  Waves,
  Zap,
  type LucideIcon,
} from "lucide-react";

import type { ActivityLevel } from "@/lib/types";
import { cn } from "@/lib/utils";

const activityIcons: Record<ActivityLevel, LucideIcon> = {
  sedentary: Armchair,
  light: Footprints,
  moderate: PersonStanding,
  high: Dumbbell,
  athlete: Flame,
};

export function ActivityIcon({
  level,
  size = 18,
  className,
}: {
  level: ActivityLevel;
  size?: number;
  className?: string;
}): JSX.Element {
  const Icon = activityIcons[level];
  return <Icon size={size} strokeWidth={2.4} className={cn("shrink-0", className)} />;
}

const exerciseIcons: Record<string, LucideIcon> = {
  Footprints,
  PersonStanding,
  Bike,
  Dumbbell,
  Zap,
  Waves,
  Flower2,
  Goal,
  Trophy,
  Sailboat,
  Swords,
  Mountain,
  Flame,
};

export function ExerciseIcon({
  name,
  size = 18,
  className,
}: {
  name: string;
  size?: number;
  className?: string;
}): JSX.Element {
  const Icon = exerciseIcons[name] ?? Flame;
  return <Icon size={size} strokeWidth={2.4} className={cn("shrink-0", className)} />;
}
