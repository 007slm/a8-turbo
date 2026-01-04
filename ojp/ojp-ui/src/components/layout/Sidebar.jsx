import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ChevronDown, ChevronRight, Menu as MenuIcon, Command, Sparkles } from 'lucide-react';
import { cn } from '../../utils/cn';
import { menuItems } from '../../config/menu';

const SidebarItem = ({ item, depth = 0, isCollapsed, activePath, onNavigate }) => {
    const [isOpen, setIsOpen] = useState(true);
    const hasChildren = item.children && item.children.length > 0;

    // Check if any child is active
    const isChildActive = hasChildren && item.children.some(child =>
        child.path === activePath || activePath.startsWith(child.path)
    );

    const isActive = item.path === activePath || activePath.startsWith(item.path + '/') || isChildActive;

    const handleClick = () => {
        if (hasChildren) {
            setIsOpen(!isOpen);
        } else if (item.path) {
            onNavigate(item.path);
        }
    };

    if (isCollapsed && depth > 0) return null;

    // Indentation for nested items
    const paddingLeft = isCollapsed ? '0.5rem' : `${depth * 10 + 10}px`;

    return (
        <div className="w-full relative mb-1">
            {/* Active Indicator Line for root items */}
            {isActive && depth === 0 && !isCollapsed && (
                <div className="absolute left-0 top-1/2 -translate-y-1/2 h-6 w-1 bg-primary rounded-r-full shadow-[0_0_12px_rgba(var(--primary),0.5)] z-10" />
            )}

            <div
                className={cn(
                    "group flex items-center h-[38px] mx-3 rounded-lg cursor-pointer transition-all duration-300 ease-out border border-transparent",
                    isActive
                        ? "bg-primary/5 text-primary font-medium border-primary/10 shadow-[0_2px_8px_rgba(0,0,0,0.02)]"
                        : "text-slate-500 hover:bg-slate-50 hover:text-slate-900",
                    isCollapsed && "justify-center px-0 mx-2 aspect-square h-auto py-2"
                )}
                style={{ paddingLeft: isCollapsed ? 0 : paddingLeft, paddingRight: '12px' }}
                onClick={handleClick}
                title={isCollapsed ? item.label : undefined}
            >
                <div className={cn("flex items-center w-full", isCollapsed ? "justify-center" : "gap-3")}>
                    <span className={cn(
                        "shrink-0 transition-all duration-300",
                        isActive ? "text-primary scale-110" : "group-hover:text-slate-700",
                        isCollapsed && "mx-auto"
                    )}>
                        {item.icon ? item.icon : <div className={cn("w-1.5 h-1.5 rounded-full transition-colors", isActive ? "bg-primary" : "bg-slate-300")} />}
                    </span>

                    {!isCollapsed && (
                        <span className="truncate tracking-wide text-[13px]">
                            {item.label}
                        </span>
                    )}

                    {!isCollapsed && hasChildren && (
                        <div className={cn(
                            "ml-auto shrink-0 transition-transform duration-300 text-slate-400 group-hover:text-slate-600",
                            isOpen ? "rotate-180" : ""
                        )}>
                            <ChevronDown size={12} strokeWidth={3} />
                        </div>
                    )}
                </div>
            </div>

            {!isCollapsed && hasChildren && (
                <div className={cn(
                    "grid transition-all duration-300 ease-[cubic-bezier(0.25,0.1,0.25,1)]",
                    isOpen ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
                )}>
                    <div className="overflow-hidden pt-1 space-y-0.5">
                        {item.children.map(child => (
                            <SidebarItem
                                key={child.key}
                                item={child}
                                depth={depth + 1}
                                isCollapsed={isCollapsed}
                                activePath={activePath}
                                onNavigate={onNavigate}
                            />
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

export default function Sidebar({ isOpen, toggleSidebar }) {
    const navigate = useNavigate();
    const location = useLocation();
    const activePath = location.pathname;

    return (
        <div
            className={cn(
                "flex flex-col h-full bg-white border-r border-slate-100 transition-all duration-500 cubic-bezier(0.4, 0, 0.2, 1) shadow-[4px_0_24px_-12px_rgba(0,0,0,0.05)] z-50",
                isOpen ? "w-[260px]" : "w-[72px]"
            )}
        >
            {/* Header / Logo Area */}
            <div className="h-[68px] flex items-center justify-between px-5 shrink-0 bg-gradient-to-b from-white to-slate-50/50">
                <div className={cn(
                    "flex items-center gap-3 overflow-hidden transition-all duration-500",
                    isOpen ? "opacity-100 translate-x-0 w-full" : "opacity-0 -translate-x-4 w-0"
                )}>
                    <div className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 text-white shadow-lg shadow-blue-500/25 ring-1 ring-black/5">
                        <Command className="h-5 w-5" strokeWidth={2.5} />
                        <div className="absolute inset-0 rounded-xl ring-1 ring-inset ring-white/20"></div>
                    </div>
                    <div className="flex flex-col justify-center">
                        <span className="font-bold text-[15px] leading-tight text-slate-900 tracking-tight">A8 Platform</span>
                        <div className="flex items-center gap-1">
                            <span className="flex h-1.5 w-1.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.4)]"></span>
                            <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest">Enterprise</span>
                        </div>
                    </div>
                </div>

                <button
                    onClick={toggleSidebar}
                    className={cn(
                        "p-2 rounded-lg text-slate-400 hover:bg-white hover:text-slate-600 hover:shadow-md hover:shadow-slate-200/50 transition-all duration-300 border border-transparent hover:border-slate-100",
                        !isOpen && "mx-auto"
                    )}
                >
                    <MenuIcon className="h-5 w-5" strokeWidth={2} />
                </button>
            </div>

            {/* Menu Items */}
            <div className="flex-1 overflow-y-auto py-5 px-0 custom-scrollbar">
                <div className="px-3 mb-2">
                    {isOpen && (
                        <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest pl-3 mb-2 flex items-center gap-2">
                            <Sparkles className="w-3 h-3 text-indigo-400" />
                            <span>Overview</span>
                        </div>
                    )}
                </div>

                <div className="space-y-0.5">
                    {menuItems.map(item => (
                        <SidebarItem
                            key={item.key}
                            item={item}
                            isCollapsed={!isOpen}
                            activePath={activePath}
                            onNavigate={(path) => navigate(path)}
                        />
                    ))}
                </div>
            </div>

            {/* Footer */}
            <div className="p-4 border-t border-slate-100 shrink-0 bg-slate-50/30">
                {isOpen ? (
                    <div className="flex items-center gap-3 p-2 rounded-xl border border-slate-100 bg-white shadow-sm hover:shadow-md hover:border-slate-200 transition-all duration-300 group cursor-pointer">
                        <div className="h-9 w-9 rounded-full bg-gradient-to-br from-indigo-100 to-white border border-indigo-50 flex items-center justify-center text-xs font-bold text-indigo-600 group-hover:scale-105 transition-transform duration-300 shadow-sm">
                            A8
                        </div>
                        <div className="flex flex-col overflow-hidden">
                            <span className="text-sm font-semibold text-slate-700 truncate group-hover:text-primary transition-colors">Admin User</span>
                            <span className="text-[10px] font-medium text-slate-400 truncate">Pro License</span>
                        </div>
                        <ChevronRight className="ml-auto w-4 h-4 text-slate-300 group-hover:text-primary transition-colors" />
                    </div>
                ) : (
                    <div className="flex justify-center">
                        <div className="h-9 w-9 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-xs font-bold text-slate-500 hover:bg-white hover:border-indigo-200 hover:text-indigo-600 shadow-sm transition-all duration-300 cursor-pointer">
                            A8
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
