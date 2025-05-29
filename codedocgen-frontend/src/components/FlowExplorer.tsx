import React, { useState } from 'react';

interface FlowStep {
  // Define structure of a step, e.g.,
  id: string;
  action: string;
  details?: string;
}
interface Flow {
  name: string;
  description?: string;
  steps?: FlowStep[] | string[]; // Can be an array of objects or simple strings
}

interface FlowExplorerProps {
  flows: Flow[];
}

const FlowExplorer: React.FC<FlowExplorerProps> = ({ flows }) => {
  const [expandedFlowIndex, setExpandedFlowIndex] = useState<number | null>(null);

  if (!flows || flows.length === 0) {
    return <p>No call flows available to display.</p>;
  }

  const toggleFlowExpansion = (index: number) => {
    setExpandedFlowIndex(expandedFlowIndex === index ? null : index);
  };

  return (
    <div>
      <h2 className="text-xl font-semibold mb-2">Call Flows / Feature Traces</h2>
      {/* 
        This component will need a more sophisticated way to display flows.
        Could be a tree view, a series of nested lists, or a graphical representation if simple enough.
        For now, a simple list of flow starting points or summaries.
      */}
      <ul>
        {flows.map((flow, index) => (
          <li key={index} className="border-b py-2">
            <div onClick={() => toggleFlowExpansion(index)} style={{ cursor: 'pointer' }}>
              <h3 className="font-medium">{flow.name || 'Unnamed Flow'}</h3>
              <p className="text-sm text-gray-600">{flow.description || 'No description.'}</p>
            </div>
            {/* TODO: Add more details or a way to expand/explore the flow - Implemented basic expand/collapse */}
            {expandedFlowIndex === index && flow.steps && (
              <ul className="mt-2 pl-4 list-disc list-inside bg-gray-50 p-2 rounded">
                {flow.steps.map((step, stepIndex) => (
                  <li key={stepIndex} className="text-sm">
                    {typeof step === 'string' ? step : `${step.action} ${step.details ? '('+step.details+')' : ''}`}
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default FlowExplorer; 