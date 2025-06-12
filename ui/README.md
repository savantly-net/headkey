# HeadKey UI - Memory Ingestion Interface

A modern Next.js application providing a user-friendly interface for the HeadKey Memory Management System. Built with React, TypeScript, Tailwind CSS, and shadcn/ui components.

## Overview

The HeadKey UI allows users to interact with the Cognitive Ingestion & Belief Formation Engine (CIBFE) through an intuitive web interface. It provides capabilities for memory ingestion, validation, system monitoring, and more.

## Features

### 🧠 Memory Ingestion
- **Interactive Form**: User-friendly interface for entering memory information
- **Real-time Validation**: Input validation with immediate feedback
- **Dry Run Testing**: Preview how memories will be processed before ingestion
- **Metadata Management**: Configure importance, tags, categories, and custom metadata
- **Batch Operations**: Support for multiple memory ingestion workflows

### 📊 System Monitoring
- **Health Checks**: Real-time system health monitoring
- **Performance Metrics**: Track ingestion rates, response times, and cache performance
- **Statistics Dashboard**: View memory counts, agent statistics, and category breakdowns
- **Auto-refresh**: Configurable automatic updates for monitoring data

### 🎨 Modern UI/UX
- **Responsive Design**: Works seamlessly across desktop, tablet, and mobile devices
- **Dark/Light Mode**: Automatic theme detection with manual override options
- **Accessibility**: WCAG compliant with proper ARIA labels and keyboard navigation
- **Toast Notifications**: Real-time feedback for user actions
- **Loading States**: Clear visual feedback during API operations

## Technology Stack

- **Framework**: Next.js 15 with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS with custom design system
- **UI Components**: shadcn/ui (Radix UI primitives)
- **Icons**: Lucide React
- **Notifications**: Sonner
- **Animations**: Framer Motion
- **Package Manager**: Multiple options (npm, yarn, pnpm, bun)

## Getting Started

### Prerequisites

- Node.js 18+ 
- One of: npm, yarn, pnpm, or bun
- HeadKey backend API running (default: http://localhost:8080)

### Installation

1. **Clone and navigate to the UI directory**:
```bash
cd headkey/ui
```

2. **Install dependencies**:
```bash
# Using npm
npm install

# Using yarn
yarn install

# Using pnpm
pnpm install

# Using bun
bun install
```

3. **Configure environment variables**:
Create a `.env.local` file in the UI directory:
```env
# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Optional: Enable development features
NODE_ENV=development
```

4. **Start the development server**:
```bash
# Using npm
npm run dev

# Using yarn
yarn dev

# Using pnpm
pnpm dev

# Using bun
bun dev
```

5. **Open your browser**:
Navigate to [http://localhost:3000](http://localhost:3000)

## Project Structure

```
headkey/ui/
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── globals.css         # Global styles and Tailwind
│   │   ├── layout.tsx          # Root layout with navigation
│   │   ├── page.tsx            # Landing page
│   │   └── ingest/             # Memory ingestion pages
│   │       └── page.tsx        # Main ingestion interface
│   ├── components/             # Reusable React components
│   │   ├── ui/                 # shadcn/ui components
│   │   ├── navigation.tsx      # Main navigation component
│   │   ├── system-monitor.tsx  # System monitoring dashboard
│   │   └── toast-config.tsx    # Toast notification configuration
│   ├── lib/                    # Utility functions and configurations
│   │   ├── api.ts              # API client and utilities
│   │   └── utils.ts            # General utility functions
│   └── hooks/                  # Custom React hooks (future)
├── public/                     # Static assets
├── package.json                # Dependencies and scripts
├── tailwind.config.js          # Tailwind CSS configuration
├── tsconfig.json              # TypeScript configuration
└── next.config.ts             # Next.js configuration
```

## Usage Guide

### Memory Ingestion

1. **Navigate to Ingestion**: Click "Start Ingesting" or use the navigation menu
2. **Fill Required Fields**:
   - **Agent ID**: Unique identifier for the agent (e.g., "user-123")
   - **Memory Content**: The information to be stored
   - **Source**: Origin of the information (e.g., "conversation", "document")

3. **Configure Metadata** (Optional):
   - **Importance**: Scale from 0.0 to 1.0
   - **Tags**: Comma-separated keywords
   - **Category**: Manual category override

4. **Validate and Test**:
   - **Validate Input**: Check for formatting and requirement issues
   - **Dry Run**: Preview processing without storing
   - **Ingest Memory**: Store the memory in the system

### System Monitoring

1. **Access Monitor Tab**: Switch to the "System Monitor" tab in the ingestion interface
2. **View Health Status**: Check overall system and component health
3. **Review Metrics**: Analyze performance statistics and trends
4. **Refresh Data**: Use manual refresh or enable auto-refresh

## API Integration

The UI communicates with the HeadKey backend through REST API endpoints:

### Memory Operations
- `POST /api/v1/memory/ingest` - Store new memories
- `POST /api/v1/memory/dry-run` - Test memory processing
- `POST /api/v1/memory/validate` - Validate input data

### System Operations
- `GET /api/v1/system/health` - System health checks
- `GET /api/v1/system/statistics` - Performance metrics
- `GET /api/v1/system/config` - System configuration

### Error Handling

The UI includes comprehensive error handling:
- **Network Errors**: Graceful handling of connection issues
- **API Errors**: Detailed error messages from backend
- **Validation Errors**: Client-side and server-side validation
- **Toast Notifications**: User-friendly error reporting

## Customization

### Theming

The application uses a custom design system built on Tailwind CSS:

- **Colors**: Defined in `globals.css` with CSS custom properties
- **Components**: Styled using shadcn/ui with custom variants
- **Dark Mode**: Automatic detection with manual override support

### API Configuration

Update API endpoints in `src/lib/api.ts`:

```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
```

### Component Customization

All UI components are in `src/components/ui/` and can be customized:

```bash
# Add new shadcn/ui components
npx shadcn-ui@latest add [component-name]
```

## Development

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run start` - Start production server
- `npm run lint` - Run ESLint

### Code Style

The project follows these conventions:
- **TypeScript**: Strict mode enabled
- **ESLint**: Next.js recommended configuration
- **Prettier**: Automatic code formatting
- **Component Structure**: Functional components with hooks

### Adding New Features

1. **API Integration**: Add endpoints to `src/lib/api.ts`
2. **UI Components**: Create in `src/components/`
3. **Pages**: Add to `src/app/` following App Router conventions
4. **Styling**: Use Tailwind classes and custom CSS properties

## Deployment

### Build for Production

```bash
npm run build
```

### Environment Variables

For production deployment, set:

```env
NEXT_PUBLIC_API_BASE_URL=https://your-api-domain.com
NODE_ENV=production
```

### Deployment Platforms

The application can be deployed to:
- **Vercel**: Optimized for Next.js applications
- **Netlify**: Static site deployment with API routes
- **Docker**: Containerized deployment
- **Traditional Hosting**: Build static files and serve

## Troubleshooting

### Common Issues

1. **API Connection Errors**:
   - Verify backend server is running
   - Check `NEXT_PUBLIC_API_BASE_URL` configuration
   - Ensure CORS is properly configured on backend

2. **Build Errors**:
   - Clear `.next` directory: `rm -rf .next`
   - Reinstall dependencies: `rm -rf node_modules && npm install`
   - Check TypeScript errors: `npm run lint`

3. **Styling Issues**:
   - Verify Tailwind CSS is properly configured
   - Check custom CSS properties in `globals.css`
   - Ensure shadcn/ui components are correctly installed

### Debug Mode

Enable debug logging by setting:
```env
NODE_ENV=development
```

## Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/new-feature`
3. **Follow coding standards**: Use TypeScript, ESLint, and Prettier
4. **Test thoroughly**: Ensure all features work with the backend API
5. **Submit a pull request**: Include detailed description of changes

## Future Enhancements

- [ ] **Memory Search Interface**: Advanced search and filtering capabilities
- [ ] **Batch Import**: File upload for bulk memory ingestion
- [ ] **Analytics Dashboard**: Detailed memory analytics and insights
- [ ] **User Management**: Multi-user support with authentication
- [ ] **Real-time Updates**: WebSocket integration for live updates
- [ ] **Mobile App**: React Native or Progressive Web App
- [ ] **API Documentation**: Interactive API explorer
- [ ] **Export Features**: Memory export in various formats

## Support

For questions, issues, or contributions:

- **GitHub Issues**: Report bugs and request features
- **Documentation**: Check the main HeadKey documentation
- **API Reference**: Refer to backend API documentation

## License

This project is part of the HeadKey Memory Management System by Savantly. All rights reserved.