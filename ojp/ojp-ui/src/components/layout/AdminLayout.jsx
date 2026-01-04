import React, { useState } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';
import { useQuery } from 'react-query';
import { fetchSystemStatus } from '../../services/api';

export default function AdminLayout({ children }) {
    const [sidebarOpen, setSidebarOpen] = useState(true);

    // Re-implementing the system status fetch here or pass it down
    // For better architecture, this should probably be in a Context or standard hook,
    // but to match App.jsx logic we'll fetch it here or passed from App.
    // For now let's re-use the hook.

    const { data: systemStatus } = useQuery(
        'systemStatus',
        fetchSystemStatus,
        {
            refetchInterval: 30000,
            refetchIntervalInBackground: true,
            retry: false
        }
    );

    return (
        <div className="flex h-screen w-screen bg-gray-50/50 overflow-hidden font-sans">
            <Sidebar isOpen={sidebarOpen} toggleSidebar={() => setSidebarOpen(!sidebarOpen)} />

            <div className="flex-1 flex flex-col h-full min-w-0 overflow-hidden">
                <Header systemStatus={systemStatus} />

                <main className="flex-1 overflow-y-auto p-6 scroll-smooth">
                    <div className="mx-auto max-w-7xl animate-in fade-in slide-in-from-bottom-4 duration-500">
                        {children}
                    </div>
                </main>
            </div>
        </div>
    );
}
