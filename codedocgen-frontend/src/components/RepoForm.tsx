import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom'; // For navigation after submission

// import { Input } from "@/components/ui/input"; // Example shadcn/ui component
// import { Button } from "@/components/ui/button"; // Example shadcn/ui component

const RepoForm: React.FC = () => {
    const [gitUrl, setGitUrl] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setIsLoading(true);
        setError(null);
        try {
            // Replace with your actual API endpoint from services/api.ts
            const response = await axios.post('http://localhost:8080/api/repo/analyze', { gitUrl }); 
            console.log('Analysis started:', response.data);
            // Navigate to dashboard or display results
            // For now, let's assume the response contains a path or ID to the results
            // And we navigate to a dashboard page, passing this identifier
            // This is a simplified example; actual navigation might depend on polling or direct data
            const { projectName } = response.data; // Assuming backend sends back some immediate data
            // In a real scenario, the backend might return a task ID or a direct path to the cloned repo
            // For this placeholder, let's simulate navigating with a conceptual repoPath
            // The actual repoPath would come from where GitService clones the repo on the backend
            const repoName = gitUrl.substring(gitUrl.lastIndexOf('/') + 1).replace(".git", "");
            navigate(`/dashboard?repoPath=${encodeURIComponent(repoName)}`); // Example navigation

        } catch (err: any) {
            console.error('Error submitting repository URL:', err);
            setError(err.response?.data?.message || err.message || 'Failed to start analysis.');
        }
        setIsLoading(false);
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label htmlFor="gitUrl" className="block text-sm font-medium text-gray-700 mb-1">
                    Git Repository URL:
                </label>
                {/* Replace with shadcn/ui Input component */}
                <input
                    type="url"
                    id="gitUrl"
                    value={gitUrl}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setGitUrl(e.target.value)}
                    placeholder="https://github.com/user/repo.git"
                    required
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    disabled={isLoading}
                />
            </div>
            {/* Replace with shadcn/ui Button component */}
            <button
                type="submit"
                disabled={isLoading}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
            >
                {isLoading ? 'Analyzing...' : 'Analyze Repository'}
            </button>
            {error && <p className="text-red-500 text-sm">{error}</p>}
        </form>
    );
};

export default RepoForm; 