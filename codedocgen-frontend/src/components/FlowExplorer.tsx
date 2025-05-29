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

const formatFlowSteps = (flowName: string, flowDescription: string, steps: FlowStep[] | string[]): string => {
  let output = `Feature Trace: ${flowName}\n`;
  output += `Initial Call: ${flowDescription.replace('Call flow starting from ', '').replace(/\(.*\)/, '()')}\n\n`;
  output += 'Execution Steps:\n';

  steps.forEach((step, index) => {
    let stepText = typeof step === 'string' ? step : `${step.action} ${step.details ? '(' + step.details + ')' : ''}`;
    
    // Strip parameters from the method call
    stepText = stepText.replace(/\([^\)]*\)/g, '()');
    
    // Remove UNRESOLVED_CALL prefixes before stripping parameters for lines that were just UNRESOLVED_CALL markers
    if (stepText.startsWith('UNRESOLVED_CALL: UNRESOLVED_CALL:')) {
      stepText = stepText.replace('UNRESOLVED_CALL: UNRESOLVED_CALL: ', '');
    } else if (stepText.startsWith('UNRESOLVED_CALL:')) {
      stepText = stepText.replace('UNRESOLVED_CALL: ', '');
    }
    
    // Re-apply parameter stripping in case the prefix removal exposed parameters (e.g. for Customer.builder().firstName(UNKNOWN_PARAM_TYPE) )
    stepText = stepText.replace(/\([^\)]*\)/g, '()');
    // Special handling for builder pattern where parameters might be part of the fluent chain
    // This regex tries to find .methodName(ANYTHING_NOT_CLOSING_PARENTHESIS)
    // and replaces it with .methodName()
    // It's a bit more aggressive to catch chained calls that might have complex arguments.
    stepText = stepText.replace(/\.([a-zA-Z0-9_]+)\([^)]*\)/g, '.\$1()');


    output += `${index + 1}. ${stepText}\n`;
  });
  return output;
};

const FlowExplorer: React.FC<FlowExplorerProps> = ({ flows }) => {
  const [expandedFlowIndex, setExpandedFlowIndex] = useState<number | null>(null);
  const [copiedStates, setCopiedStates] = useState<{ [key: number]: boolean }>({});

  if (!flows || flows.length === 0) {
    return <p>No call flows available to display.</p>;
  }

  const toggleFlowExpansion = (index: number) => {
    setExpandedFlowIndex(expandedFlowIndex === index ? null : index);
  };

  const handleCopy = (textToCopy: string, index: number) => {
    navigator.clipboard.writeText(textToCopy).then(() => {
      setCopiedStates(prev => ({ ...prev, [index]: true }));
      setTimeout(() => {
        setCopiedStates(prev => ({ ...prev, [index]: false }));
      }, 2000);
    }).catch(err => {
      console.error('Failed to copy text: ', err);
      // Optionally, provide user feedback about the copy failure
    });
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
              <div className="mt-2 pl-4 bg-gray-50 p-3 rounded shadow">
                <button
                  onClick={() => handleCopy(formatFlowSteps(flow.name || 'Unnamed Flow', flow.description || '', flow.steps || []), index)}
                  className="mb-2 px-3 py-1 bg-blue-500 hover:bg-blue-600 text-white text-sm rounded shadow transition-colors duration-150"
                >
                  {copiedStates[index] ? 'Copied!' : 'Copy Trace'}
                </button>
                <pre className="text-sm whitespace-pre-wrap break-all overflow-x-auto p-2 border border-gray-300 rounded bg-white">
                  {formatFlowSteps(flow.name || 'Unnamed Flow', flow.description || '', flow.steps || [])}
                </pre>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default FlowExplorer; 