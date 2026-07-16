import type { SVGProps } from 'react'

export type IconProps = SVGProps<SVGSVGElement>

export type EyeIconProps = IconProps & {
  open: boolean
}

export type LogoProps = {
  compact?: boolean
}
