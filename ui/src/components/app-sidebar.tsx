"use client";

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Brain,
  Upload,
  Search,
  BarChart3,
  Settings,
  Home,
  Activity,
} from "lucide-react";

const navigationItems = [
  {
    name: "Home",
    href: "/",
    icon: Home,
    description: "HeadKey overview and getting started",
  },
  {
    name: "Ingest Memory",
    href: "/ingest",
    icon: Upload,
    description: "Add new information to the memory system",
  },
  {
    name: "Search & Query",
    href: "/search",
    icon: Search,
    description: "Find and retrieve stored memories",
    disabled: true,
  },
  {
    name: "Analytics",
    href: "/analytics",
    icon: BarChart3,
    description: "System performance and memory insights",
    disabled: true,
  },
  {
    name: "System Monitor",
    href: "/monitor",
    icon: Activity,
    description: "Health checks and system status",
    disabled: false,
  },
];

export function AppSidebar() {
  const pathname = usePathname();

  return (
    <Sidebar className="border-r border-gray-200">
      <SidebarHeader className="border-b border-gray-200">
        <div className="flex items-center gap-2 px-4 py-4">
          <Brain className="h-8 w-8 text-blue-600" />
          <span className="text-xl font-bold gradient-text">HeadKey</span>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navigationItems.map((item) => (
                <SidebarMenuItem key={item.href}>
                  <SidebarMenuButton
                    asChild
                    isActive={pathname === item.href}
                    disabled={item.disabled}
                  >
                    <Link
                      href={item.href}
                      className="flex items-center gap-3 my-2 h-14"
                    >
                      <item.icon className="h-4 w-4" />
                      <div className="flex flex-col">
                        <span className="text-sm font-medium">{item.name}</span>
                        <span className="text-xs text-gray-500">
                          {item.description}
                        </span>
                      </div>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="border-t border-gray-200">
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild>
              <Link href="/settings" className="flex items-center gap-3">
                <Settings className="h-4 w-4" />
                <span className="text-sm">Settings</span>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
