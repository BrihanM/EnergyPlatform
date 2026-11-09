export function multiply(left: string, right: string): string {
  const [a, b = ''] = left.split('.'); const [c, d = ''] = right.split('.')
  const scale = b.length + d.length
  const value = BigInt((a + b) || '0') * BigInt((c + d) || '0')
  const digits = value.toString().padStart(scale + 1, '0')
  return scale ? `${digits.slice(0, -scale)}.${digits.slice(-scale)}` : digits
}
