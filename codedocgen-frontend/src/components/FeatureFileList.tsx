import React from 'react';

// This component might list generated documentation files or feature summaries

interface FeatureFileListProps {
  files: any[]; // Replace 'any' with a type like { name: string, content?: string, downloadUrl?: string }
}

const FeatureFileList: React.FC<FeatureFileListProps> = ({ files }) => {
  if (!files || files.length === 0) {
    return <p>No feature files or documents available.</p>;
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-2">Generated Documents & Features</h2>
      <ul className="list-disc pl-5 space-y-1">
        {files.map((file: any, index) => (
          <li key={index}>
            {file.downloadUrl ? (
              <a 
                href={file.downloadUrl} 
                download={file.name} 
                className="text-blue-600 hover:underline"
              >
                {file.name}
              </a>
            ) : (
              <span>{file.name} (No download link)</span>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default FeatureFileList; 