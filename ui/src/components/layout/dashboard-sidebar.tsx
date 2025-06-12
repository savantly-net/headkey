import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarHeader,
} from "@/components/ui/sidebar";
import Link from "next/link";
import { Home, Upload, Search, BarChart3, Settings, Users } from "lucide-react";

export function DashboardSidebar() {
  return (
    <Sidebar className="h-screen sticky top-0 shadow-soft">
      <SidebarHeader>
        <div className="flex items-center gap-2 p-4">
          <Home className="h-6 w-6 text-blue-600" />
          <span className="font-bold text-lg">Dashboard</span>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <Link
            href="/"
            className="flex items-center gap-2 text-sm p-2 rounded-soft hover:bg-gray-100"
          >
            <Home className="h-4 w-4" />
            Home
          </Link>

          <Link
            href="/ingest"
            className="flex items-center gap-2 text-sm p-2 rounded-soft hover:bg-gray-100"
          >
            <Upload className="h-4 w-4" />
            Ingest Memory
          </Link>

          <Link
            href="/search"
            className="flex items-center gap-2 text-sm p-2 rounded-soft hover:bg-gray-100"
          >
            <Search className="h-4 w-4" />
            Search & Query
          </Link>

          <Link
            href="/analytics"
            className="flex items-center gap-2 text-sm p-2 rounded-soft hover:bg-gray-100"
          >
            <BarChart3 className="h-4 w-4" />
            Analytics
          </Link>

          <Link
            href="/monitor"
            className="flex items-center gap-2 text-sm p-2 rounded-soft hover:bg-gray-100"
          >
            <Users className="h-4 w-4" />
            System Monitor
          </Link>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>
        <div className="p-4 text-xs">
          <Settings className="h-4 w-4 inline mr-2" />
          Settings
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
