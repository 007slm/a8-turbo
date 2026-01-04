import React from 'react';
import { Bell, HelpCircle, User } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { menuItems } from '../../config/menu';
import { cn } from '../../utils/cn';

const StatusPill = ({ status, label }) => {
    const styles = {
        success: 'bg-green-100 text-green-700 border-green-200',
        warning: 'bg-yellow-100 text-yellow-700 border-yellow-200',
        error: 'bg-red-100 text-red-700 border-red-200',
        default: 'bg-gray-100 text-gray-700 border-gray-200'
    };

    return (
        <div className={cn("px-3 py-1 rounded-full text-xs font-medium border flex items-center gap-1.5", styles[status] || styles.default)}>
            <div className={cn("w-1.5 h-1.5 rounded-full", status === 'success' ? 'bg-green-500' : status === 'error' ? 'bg-red-500' : status === 'warning' ? 'bg-yellow-500' : 'bg-gray-400')} />
            {label}
        </div>
    );
};

export default function Header({ systemStatus }) {
    const location = useLocation();

    const getPageTitle = () => {
        // Simple BFS to find title
        const findTitle = (items, path) => {
            for (const item of items) {
                if (item.path === path || path.startsWith(item.path + '/')) { // Exact match or parent match (rough)
                    if (item.children) {
                        const childTitle = findTitle(item.children, path);
                        if (childTitle) return `${item.label} / ${childTitle}`;
                    }
                    return item.label;
                }
            }
            return null;
        }
        return findTitle(menuItems, location.pathname) || 'Dashboard';
    };

    const statusType = !systemStatus ? 'default' : systemStatus.status === 'UP' ? 'success' : 'error';
    const statusLabel = !systemStatus ? 'Checking...' : systemStatus.status === 'UP' ? 'System Normal' : 'System Error';

    return (
        <header className="h-16 bg-white/80 backdrop-blur-md border-b border-border px-6 flex items-center justify-between sticky top-0 z-10">
            <div className="flex items-center gap-4">
                <h1 className="text-lg font-semibold text-gray-800 tracking-tight">{getPageTitle()}</h1>
            </div>

            <div className="flex items-center gap-4">
                <StatusPill status={statusType} label={statusLabel} />

                <div className="h-8 w-px bg-gray-200 mx-2" />

                <button className="p-2 rounded-full hover:bg-gray-100 text-gray-500 transition-colors relative">
                    <Bell size={20} />
                    <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border border-white" />
                </button>

                <button className="p-2 rounded-full hover:bg-gray-100 text-gray-500 transition-colors">
                    <HelpCircle size={20} />
                </button>

                <button className="flex items-center gap-2 pl-2 pr-1 py-1 rounded-full hover:bg-gray-100 transition-colors ml-2 border border-transparent hover:border-gray-200">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white font-medium shadow-sm">
                        <User size={16} />
                    </div>
                </button>
            </div>
        </header>
    );
}
