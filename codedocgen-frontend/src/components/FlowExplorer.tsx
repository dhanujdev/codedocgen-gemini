import React from 'react';

interface FlowExplorerProps {
  flows: any[]; // Replace 'any' with a proper type for flow data
}

const FlowExplorer: React.FC<FlowExplorerProps> = ({ flows }) => {
  if (!flows || flows.length === 0) {
    return <p>No call flows available to display.</p>;
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-2">Call Flows / Feature Traces</h2>
      {/* 
        This component will need a more sophisticated way to display flows.
        Could be a tree view, a series of nested lists, or a graphical representation if simple enough.
        For now, a simple list of flow starting points or summaries.
      */}
      <ul>
        {flows.map((flow: any, index) => (
          <li key={index} className="border-b py-2">
            <h3 className="font-medium">{flow.name || 'Unnamed Flow'}</h3>
            <p className="text-sm text-gray-600">{flow.description || 'No description.'}</p>
            {/* TODO: Add more details or a way to expand/explore the flow */}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default FlowExplorer; 