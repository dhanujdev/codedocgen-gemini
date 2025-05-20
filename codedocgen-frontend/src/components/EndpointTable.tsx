import React from 'react';
// import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"; // shadcn/ui
// import { ParsedEndpoint } from '../types/ParsedData'; // Assuming this type exists

interface EndpointTableProps {
  endpoints: any[]; // Replace 'any' with ParsedEndpoint[] once type is defined
}

const EndpointTable: React.FC<EndpointTableProps> = ({ endpoints }) => {
  if (!endpoints || endpoints.length === 0) {
    return <p>No endpoints found.</p>;
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-2">API Endpoints</h2>
      {/* Replace with shadcn/ui Table component */}
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Method</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Path</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
            {/* Add more columns as needed, e.g., Request Body, Response Body */}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {endpoints.map((endpoint, index) => (
            <tr key={index}>
              <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{endpoint.httpMethod}</td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{endpoint.path}</td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{endpoint.description || 'N/A'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default EndpointTable; 