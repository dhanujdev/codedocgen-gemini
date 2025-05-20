import React, { useState } from 'react';
// import { Button } from "@/components/ui/button";
// import { Input } from "@/components/ui/input";
// import { Textarea } from "@/components/ui/textarea";

interface ConfluencePublisherProps {
  pageTitle: string;
  pageContent: string; // Markdown or Confluence-formatted content
  spaceKey: string; // Confluence Space Key
  parentId?: string; // Optional: Parent Page ID for publishing under an existing page
}

const ConfluencePublisher: React.FC<ConfluencePublisherProps> = (props: ConfluencePublisherProps) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // These would typically be fetched or configured
  const [confluenceUrl, setConfluenceUrl] = useState(''); // e.g., https://your-domain.atlassian.net/wiki
  const [username, setUsername] = useState('');
  const [apiToken, setApiToken] = useState('');

  const handlePublish = async () => {
    setIsLoading(true);
    setError(null);
    setSuccessMessage(null);

    if (!confluenceUrl || !username || !apiToken) {
        setError("Confluence URL, Username, and API Token are required.");
        setIsLoading(false);
        return;
    }

    // In a real app, this API call would be in a service (e.g., services/confluenceService.ts)
    // And would use a library like Axios, handling authentication properly (Basic Auth with API token)
    console.log("Publishing to Confluence:", props);
    // Simulate API call
    try {
        // const response = await axios.post(`${confluenceUrl}/rest/api/content`, 
        //     {
        //         type: 'page',
        //         title: props.pageTitle,
        //         space: { key: props.spaceKey },
        //         body: {
        //             storage: {
        //                 value: props.pageContent, // Convert Markdown to Confluence storage format if needed
        //                 representation: 'storage' // or 'wiki' if sending wiki markup
        //             }
        //         },
        //         ...(props.parentId && { ancestors: [{ id: props.parentId }] })
        //     },
        //     {
        //         auth: {
        //             username: username,
        //             password: apiToken
        //         },
        //         headers: {
        //             'Accept': 'application/json',
        //             'Content-Type': 'application/json'
        //         }
        //     }
        // );
        // console.log("Confluence publish response:", response.data);
        // setSuccessMessage(`Successfully published page: ${response.data.title} (ID: ${response.data.id})`);
        await new Promise(resolve => setTimeout(resolve, 1500)); // Simulate delay
        setSuccessMessage(`Successfully published page: ${props.pageTitle} (Simulated)`);

    } catch (err: any) {
        console.error("Failed to publish to Confluence:", err);
        setError(err.response?.data?.message || err.message || 'Failed to publish.');
    }
    setIsLoading(false);
  };

  return (
    <div className="space-y-4 p-4 border rounded-md">
        <h3 className="text-lg font-medium">Publish to Confluence</h3>
        {/* Add Input fields for confluenceUrl, username, apiToken, props.spaceKey, props.parentId if they are user-configurable at publish time */}
        <div>
            <label htmlFor="confluenceUrl" className="block text-sm font-medium">Confluence URL</label>
            <input type="url" id="confluenceUrl" value={confluenceUrl} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setConfluenceUrl(e.target.value)} className="mt-1 block w-full" placeholder="https://your-domain.atlassian.net/wiki" />
        </div>
        <div>
            <label htmlFor="username" className="block text-sm font-medium">Username (Email)</label>
            <input type="text" id="username" value={username} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setUsername(e.target.value)} className="mt-1 block w-full" />
        </div>
        <div>
            <label htmlFor="apiToken" className="block text-sm font-medium">API Token</label>
            <input type="password" id="apiToken" value={apiToken} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setApiToken(e.target.value)} className="mt-1 block w-full" />
        </div>
      
        <button 
            onClick={handlePublish} 
            disabled={isLoading}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
        >
            {isLoading ? 'Publishing...' : 'Publish to Confluence'}
        </button>
        {successMessage && <p className="text-green-600 text-sm">{successMessage}</p>}
        {error && <p className="text-red-500 text-sm">{error}</p>}
    </div>
  );
};

export default ConfluencePublisher; 