import { execSync } from 'child_process';

console.log('Installing GridScan component...');
try {
  execSync('pnpm dlx shadcn@latest add @react-bits/GridScan-JS-CSS', {
    stdio: 'inherit',
    cwd: process.cwd()
  });
  console.log('GridScan installed successfully!');
} catch (error) {
  console.error('Failed to install GridScan:', error);
  process.exit(1);
}
