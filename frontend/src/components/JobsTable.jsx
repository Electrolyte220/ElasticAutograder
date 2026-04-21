import { downloadResults } from "../api/download_file";
import { fetchFileNameFromId } from "../api/get_filename_from_id";
import { useMemo } from "react";
import { AgGridProvider, AgGridReact } from "ag-grid-react";
import { AllCommunityModule } from "ag-grid-community";
import { themeQuartz } from "ag-grid-community";

const theme = themeQuartz.withParams({
  backgroundColor: "#1f2836",
  browserColorScheme: "dark",
  chromeBackgroundColor: {
    ref: "foregroundColor",
    mix: 0.07,
    onto: "backgroundColor"
  },
  foregroundColor: "#FFF",
  headerFontSize: 14
});

let currentPageSize = 5;

export default function JobsTable({ jobs, gridRef, onViewDetails }) {
  const colDefs = useMemo(() => [
    { field: "id", headerName: "ID", cellDataType: "number", width: 100},
    { field: "graderType", headerName: "Grader Type", valueFormatter: ({ value }) => value ?? "", width: 125},
    { field: "originalFilename", headerName: "Filename", valueFormatter: ({ value }) => value ?? "", width: 150},
    { field: "status", headerName: "Status", valueFormatter: ({ value }) => value ?? "", width: 125},
    { field: "createdAt", headerName: "Created At", valueFormatter: ({ value }) => value ? formatDate(value) : "", width: 240,
      filterParams: {
        includeTime: true,
        comparator: (d1, d2) => new Date(d2).getTime() - new Date(d1).getTime()
      }
    },
    { field: "score", headerName: "Score", cellDataType: "number", valueFormatter: ({ value }) => value ?? "", width: 100},
    { field: "tests", headerName: "Tests", valueGetter: ({ data }) => formatTests(data), width: 100},
    {
      headerName: "Actions",
      cellRenderer: (params) => actionsCellRenderer({ ...params, onViewDetails }),
      sortable: false,
      filter: false,
      width: 260,
      flex: 0
    }
  ], [onViewDetails]);

  return (
    <AgGridProvider modules={[AllCommunityModule]}>
      <div>
        <AgGridReact
          ref={gridRef}
          rowData={jobs}
          columnDefs={colDefs}
          defaultColDef={{ filter: true, flex: 0, floatingFilter: true, suppressMovable: true}}
          gridOptions={{
            theme: theme,
            pagination: true,
            paginationPageSize: currentPageSize,
            paginationPageSizeSelector: [5, 10, 25, 100],
            onPaginationChanged: (p) => {
              currentPageSize = p.api.paginationGetPageSize()
            },
            domLayout: "autoHeight"
          }}
          getRowId={({ data }) => String(data.id)}
        />
      </div>
    </AgGridProvider>
  );
}

function formatDate(date) {
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "numeric",
    second: "numeric"
  }).format(new Date(date));
}

function formatTests(data) {
  return data.testsPassed != null && data.testsTotal != null
    ? `${data.testsPassed} / ${data.testsTotal}`
    : "";
}

function actionsCellRenderer({ data, onViewDetails }) {
  const canDownload = data.status === "SUCCEEDED";

  return (
    <div style={{ display: "flex", gap: "8px", alignItems: "center", height: "100%" }}>
      <button
        onClick={() => onViewDetails?.(data)}
        style={{
          padding: "6px 10px",
          borderRadius: "4px",
          border: "none",
          cursor: "pointer"
        }}
      >
        Details
      </button>

      <button
        onClick={() => handleDownload(data.id)}
        disabled={!canDownload}
        title={canDownload ? "Download results.json" : "No downloadable results available"}
        style={{
          padding: "6px 10px",
          borderRadius: "4px",
          border: "none",
          cursor: canDownload ? "pointer" : "not-allowed",
          opacity: canDownload ? 1 : 0.5
        }}
      >
        Download Results
      </button>
    </div>
  );
}

const handleDownload = async (id) => {
  try {
    let name = await fetchFileNameFromId(id);
    name = name.substring(0, name.length - 3);
    const blob = await downloadResults(id);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${name}-results.json`;
    a.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    alert("Could not download results file.");
    throw new Error("Could not download results file.\n" + err);
  }
};