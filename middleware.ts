export { default } from 'next-auth/middleware'

export const config = {
  matcher: ['/dashboard/:path*', '/session/:path*', '/history/:path*'],
}
